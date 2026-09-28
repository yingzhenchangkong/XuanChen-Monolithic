package com.xuanchen.common.config;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Mybatis拦截器 自动注入 创建人 创建时间 更新者 更新时间
 *
 * <p>覆盖三类写入链路：</p>
 * <ol>
 *     <li>INSERT：实体的 createBy/createTime/updateBy/updateTime 为空时统一填充；</li>
 *     <li>UPDATE 实体参数（updateById / update(entity, wrapper)）：反射强制覆盖 updateBy/updateTime；</li>
 *     <li>UPDATE 仅 Wrapper 参数（service.update(new UpdateWrapper())，entity 为 null）：
 *     按 Mapper 泛型解析目标实体类，仅当实体存在审计字段时向 Wrapper 追加参数化 SET 子句，
 *     避免给没有审计列的表（如 sys_notice_status）拼出非法列。</li>
 * </ol>
 *
 * @author XuanChen
 * @date 2025-04-11
 */
@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class MybatisInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(MybatisInterceptor.class);

    private static final String CREATE_BY = "createBy";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_BY = "updateBy";
    private static final String UPDATE_TIME = "updateTime";

    /** 实体类 -> 全部字段（含父类）反射缓存 */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    /** Mapper 类 -> 解析出的 BaseMapper 实体泛型缓存（无法解析时为 null） */
    private static final Map<Class<?>, Class<?>> ENTITY_OF_MAPPER_CACHE = new ConcurrentHashMap<>();

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        Object parameter = invocation.getArgs()[1];
        if (parameter == null) {
            return invocation.proceed();
        }
        String username = this.getLoginUser();
        LocalDateTime now = LocalDateTime.now();
        try {
            if (SqlCommandType.INSERT == sqlCommandType) {
                this.fillOnInsert(parameter, username, now);
            } else if (SqlCommandType.UPDATE == sqlCommandType) {
                this.fillOnUpdate(mappedStatement, parameter, username, now);
            }
        } catch (Throwable t) {
            //审计填充失败只记录，不阻断业务写入；但不再静默吞掉，便于发现反射/字段变更问题
            log.warn("审计字段自动填充失败 ms={} type={}", mappedStatement.getId(), sqlCommandType, t);
        }
        return invocation.proceed();
    }

    /**
     * INSERT：仅当字段为空时填充创建/更新审计字段
     */
    private void fillOnInsert(Object parameter, String username, LocalDateTime now) {
        Map<String, Field> fields = getFieldMap(parameter.getClass());
        setIfNull(parameter, fields.get(CREATE_BY), username);
        setIfNull(parameter, fields.get(CREATE_TIME), now);
        setIfNull(parameter, fields.get(UPDATE_BY), username);
        setIfNull(parameter, fields.get(UPDATE_TIME), now);
    }

    /**
     * UPDATE：
     * 1) 实体参数（et 非空）：反射强制写入 updateBy/updateTime；
     * 2) UpdateWrapper 参数：目标实体含对应字段时追加参数化 SET。
     */
    private void fillOnUpdate(MappedStatement ms, Object parameter, String username, LocalDateTime now) {
        Object entity = null;
        UpdateWrapper<?> wrapper = null;
        if (parameter instanceof MapperMethod.ParamMap<?> paramMap) {
            //注意：MapperMethod.ParamMap.get 对不存在的键会抛 BindingException，必须先 containsKey
            if (paramMap.containsKey("et") && paramMap.get("et") != null) {
                entity = paramMap.get("et");
            }
            if (paramMap.containsKey("ew") && paramMap.get("ew") instanceof UpdateWrapper<?> uw) {
                wrapper = uw;
            }
        } else if (parameter instanceof UpdateWrapper<?> uw) {
            wrapper = uw;
        } else {
            entity = parameter;
        }

        if (entity != null) {
            Map<String, Field> fields = getFieldMap(entity.getClass());
            setForce(entity, fields.get(UPDATE_BY), username);
            setForce(entity, fields.get(UPDATE_TIME), now);
        }

        if (wrapper != null) {
            Class<?> entityClass = resolveEntityClass(ms);
            if (entityClass != null) {
                Map<String, Field> fields = getFieldMap(entityClass);
                String sqlSet = wrapper.getSqlSet() == null ? "" : wrapper.getSqlSet();
                if (fields.containsKey(UPDATE_BY) && !containsColumnSet(sqlSet, "update_by")) {
                    wrapper.set("update_by", username);
                }
                if (fields.containsKey(UPDATE_TIME) && !containsColumnSet(sqlSet, "update_time")) {
                    wrapper.set("update_time", now);
                }
            }
        }
    }

    /**
     * 判断 SET 片段中是否已包含指定列，避免重复 SET 同名列导致 SQL 错误
     */
    private boolean containsColumnSet(String sqlSet, String column) {
        return Pattern.compile("(^|,)\\s*" + Pattern.quote(column) + "\\s*=", Pattern.CASE_INSENSITIVE)
                .matcher(sqlSet).find();
    }

    /**
     * 从 MappedStatement 的 Mapper 接口解析 BaseMapper 的实体泛型。
     * 仅用于判断目标表是否存在审计字段，解析失败返回 null（跳过 wrapper 填充）。
     */
    private Class<?> resolveEntityClass(MappedStatement ms) {
        String msId = ms.getId();
        int dot = msId.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String mapperClassName = msId.substring(0, dot);
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> mapperClass = Class.forName(mapperClassName, false, classLoader);
            return ENTITY_OF_MAPPER_CACHE.computeIfAbsent(mapperClass, this::findBaseMapperEntityType);
        } catch (Throwable t) {
            log.debug("解析 Mapper 实体泛型失败 mapper={}", mapperClassName, t);
            return null;
        }
    }

    /**
     * 沿接口继承链查找 BaseMapper&lt;T&gt; 的 T
     */
    private Class<?> findBaseMapperEntityType(Class<?> mapperClass) {
        Set<Type> visited = new HashSet<>();
        return findBaseMapperEntityType(mapperClass, visited);
    }

    private Class<?> findBaseMapperEntityType(Type type, Set<Type> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        if (type instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            String rawName = raw.getTypeName();
            if (rawName.endsWith(".BaseMapper") || rawName.endsWith("BaseMapper")) {
                Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class<?> clazz) {
                    return clazz;
                }
            }
            if (raw instanceof Class<?> rawClass) {
                for (Type superInterface : rawClass.getGenericInterfaces()) {
                    Class<?> found = findBaseMapperEntityType(superInterface, visited);
                    if (found != null) {
                        return found;
                    }
                }
            }
        } else if (type instanceof Class<?> clazz) {
            for (Type superInterface : clazz.getGenericInterfaces()) {
                Class<?> found = findBaseMapperEntityType(superInterface, visited);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 获取登录用户（无登录上下文时返回空串，与历史行为保持一致）
     */
    private String getLoginUser() {
        String username = "";
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                username = authentication.getName();
            }
        } catch (Exception ex) {
            log.debug("获取登录用户失败", ex);
            username = "";
        }
        return username;
    }

    /**
     * 字段为空（null 或空串）时才写入
     */
    private void setIfNull(Object target, Field field, Object value) {
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            Object current = field.get(target);
            if (current == null || "".equals(current)) {
                field.set(target, value);
            }
        } catch (IllegalAccessException e) {
            log.warn("审计字段赋值失败 field={} entity={}", field.getName(), target.getClass().getName(), e);
        }
    }

    /**
     * 强制写入（更新场景总是覆盖）
     */
    private void setForce(Object target, Field field, Object value) {
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            log.warn("审计字段赋值失败 field={} entity={}", field.getName(), target.getClass().getName(), e);
        }
    }

    /**
     * 获取类的所有属性（含父类），按字段名索引并缓存
     */
    private Map<String, Field> getFieldMap(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fieldList = new ArrayList<>();
            Class<?> current = c;
            while (current != null) {
                fieldList.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            Map<String, Field> map = new java.util.HashMap<>();
            for (Field field : fieldList) {
                map.putIfAbsent(field.getName(), field);
            }
            return map;
        });
    }
}
