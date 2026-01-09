package com.xuanchen.auth.config;

import com.xuanchen.auth.auth.entity.Auth;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Mybatis拦截器 自动注入 创建人 创建时间 更新者 更新时间
 *
 * @author XuanChen
 * @date 2025-04-11
 */
@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class MybatisInterceptor implements Interceptor {
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
        if (SqlCommandType.INSERT == sqlCommandType) {
            Auth authUser = this.getLoginUser();
            Field[] fields = this.getAllFields(parameter);
            for (Field field : fields) {
                try {
                    // 注入创建时间
                    if ("createBy".equals(field.getName())) {
                        field.setAccessible(true);
                        Object localCreateBy = field.get(parameter);
                        field.setAccessible(false);
                        if (localCreateBy == null || "".equals(localCreateBy)) {
                            if (authUser != null) {
                                // 登录人账号
                                field.setAccessible(true);
                                field.set(parameter, authUser.getUserName());
                                field.setAccessible(false);
                            }
                        }
                    }
                    // 注入创建时间
                    if ("createTime".equals(field.getName())) {
                        field.setAccessible(true);
                        Object localCreateDate = field.get(parameter);
                        field.setAccessible(false);
                        if (localCreateDate == null || "".equals(localCreateDate)) {
                            field.setAccessible(true);
                            field.set(parameter, LocalDateTime.now());
                            field.setAccessible(false);
                        }
                    }
                } catch (Exception ex) {

                }
            }
        }
        if (SqlCommandType.UPDATE == sqlCommandType) {
            Auth authUser = this.getLoginUser();
            Field[] fields = null;
            if (parameter instanceof MapperMethod.ParamMap) {
                MapperMethod.ParamMap<?> p = (MapperMethod.ParamMap<?>) parameter;
                String et = "et";
                if (p.containsKey(et)) {
                    parameter = p.get(et);
                } else {
                    parameter = p.get("param1");
                }
                if (parameter == null) {
                    return invocation.proceed();
                }
                fields = this.getAllFields(parameter);
            } else {
                fields = this.getAllFields(parameter);
            }
            for (Field field : fields) {
                try {
                    if ("updateBy".equals(field.getName())) {
                        //获取登录用户信息
                        if (authUser != null) {
                            // 登录账号
                            field.setAccessible(true);
                            field.set(parameter, authUser.getUserName());
                            field.setAccessible(false);
                        }
                    }
                    if ("updateTime".equals(field.getName())) {
                        field.setAccessible(true);
                        field.set(parameter, LocalDateTime.now());
                        field.setAccessible(false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return invocation.proceed();
    }

    /**
     * 获取登录用户
     *
     * @return
     */
    private Auth getLoginUser() {
        Auth authUser = null;
        try {
            authUser = SecurityUtils.getSubject().getPrincipal() != null ? (Auth) SecurityUtils.getSubject().getPrincipal() : null;
        } catch (Exception ex) {
            authUser = null;
        }
        return authUser;
    }

    /**
     * 获取类的所有属性 包括父类
     *
     * @param object
     * @return
     */
    private Field[] getAllFields(Object object) {
        Class<?> clazz = object.getClass();
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null) {
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        fieldList.toArray(fields);
        return fields;
    }
}