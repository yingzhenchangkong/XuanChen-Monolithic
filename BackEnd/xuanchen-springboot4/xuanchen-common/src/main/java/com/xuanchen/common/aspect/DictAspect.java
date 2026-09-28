package com.xuanchen.common.aspect;

import com.baomidou.mybatisplus.core.metadata.IPage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.xuanchen.common.api.DictApi.service.IDictApiService;
import com.xuanchen.common.aspect.annotation.Dict;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AOP切面-->字典
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DictAspect {
    private final IDictApiService dictApiService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /**
     * 定义切点Pointcut
     */
    //@Pointcut("execution(public * com.xuanchen..*.*Controller.*(..)) || @annotation(com.xuanchen.common.aspect.annotation.DictMethod)") //所有Controller添加此切点，标注了@AuotDict注解的添加此切点
    @Pointcut("@annotation(com.xuanchen.common.aspect.annotation.DictMethod)")
    public void annotationPointcut() {
    }

    @Around("annotationPointcut()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        result = parseDictText(result);
        return result;
    }

    /**
     * 从返回值中安全提取分页对象。
     * 兼容：Result&lt;IPage&gt;、含 data 键的 Map、裸 IPage；其余形态（非分页/无数据）一律返回 null，不做强转。
     */
    private IPage<?> extractPage(Object result) {
        if (result instanceof IPage<?> page) {
            return page;
        }
        Object data = null;
        if (result instanceof Result<?> r) {
            data = r.getData();
        } else if (result instanceof Map<?, ?> map) {
            data = map.get("data");
        }
        return data instanceof IPage<?> page ? page : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object parseDictText(Object result) {
        IPage page = extractPage(result);
        if (page == null) {
            return result;
        }
        List listRecords = page.getRecords();
        if (listRecords == null || listRecords.isEmpty() || !checkHasDict(listRecords)) {
            return result;
        }
        // 同一分页结果是同类型对象，字典字段只取一次
        List<Field> dictFields = getDictFields(listRecords.get(0));

        // 1、按 (表,code列,text列) 分组，从原始实体反射收集全部待翻译值（不改变实体本身）
        Map<DictGroup, Set<String>> groupValues = new LinkedHashMap<>();
        for (Object record : listRecords) {
            for (Field field : dictFields) {
                String codeValue = getFieldValue(field, record);
                if (StringUtil.isNotEmpty(codeValue)) {
                    Dict dict = field.getAnnotation(Dict.class);
                    DictGroup group = new DictGroup(dict.dictTable(), dict.dicCode(), dict.dicText());
                    groupValues.computeIfAbsent(group, k -> new LinkedHashSet<>()).add(codeValue);
                }
            }
        }

        // 2、每组只做一次 IN 批量查询，构建 code -> text 映射（字典字段组数为常数，与记录数无关）
        Map<DictGroup, Map<String, String>> groupMappings = new HashMap<>();
        for (Map.Entry<DictGroup, Set<String>> entry : groupValues.entrySet()) {
            DictGroup group = entry.getKey();
            List<Map<String, Object>> rows = dictApiService.translateFieldBatch(
                    group.table(), group.code(), group.text(), new ArrayList<>(entry.getValue()));
            Map<String, String> codeToText = new HashMap<>();
            for (Map<String, Object> row : rows) {
                String codeValue = getStringIgnoreCase(row, "dict_code_val");
                String textValue = getStringIgnoreCase(row, "dict_text_val");
                if (codeValue != null) {
                    codeToText.put(codeValue, textValue);
                }
            }
            groupMappings.put(group, codeToText);
        }

        // 3、回填 字段名_dictText。
        // 通过 Jackson 把实体转换为 Map，使 @JsonFormat/@JsonProperty 等序列化契约照常生效，
        // 不再用 fastjson JSONObject 直接替换实体（旧写法会丢失日期格式等契约）。
        List<Object> listRecordsNew = new ArrayList<>(listRecords.size());
        for (Object record : listRecords) {
            Object output = record;
            try {
                LinkedHashMap<String, Object> item = objectMapper.convertValue(record, MAP_TYPE);
                for (Field field : dictFields) {
                    Dict dict = field.getAnnotation(Dict.class);
                    DictGroup group = new DictGroup(dict.dictTable(), dict.dicCode(), dict.dicText());
                    String codeValue = getFieldValue(field, record);
                    String textValue = null;
                    if (StringUtil.isNotEmpty(codeValue)) {
                        textValue = groupMappings.getOrDefault(group, Collections.emptyMap()).get(codeValue);
                    }
                    item.put(field.getName() + "_dictText", textValue);
                }
                output = item;
            } catch (IllegalArgumentException e) {
                // 单条转换失败不影响整体响应，保留原始实体
                log.warn("字典翻译结果转换失败，保留原始实体 type={}", record.getClass().getName(), e);
            }
            listRecordsNew.add(output);
        }
        page.setRecords(listRecordsNew);
        return result;
    }

    /**
     * 反射读取字段值（字段在扫描时已 setAccessible）
     */
    private String getFieldValue(Field field, Object target) {
        try {
            Object value = field.get(target);
            return value == null ? null : String.valueOf(value);
        } catch (IllegalAccessException e) {
            log.warn("字典字段读取失败 field={}", field.getName(), e);
            return null;
        }
    }

    /**
     * 获取对象上所有标注了 {@link Dict} 的字段（含父类）
     */
    private List<Field> getDictFields(Object object) {
        List<Field> dictFields = new ArrayList<>();
        for (Field field : getAllFields(object)) {
            if (field.getAnnotation(Dict.class) != null) {
                dictFields.add(field);
            }
        }
        return dictFields;
    }

    /**
     * JDBC 对 Map 结果键的大小写处理在不同驱动下不一致，做大小写兼容读取
     */
    private String getStringIgnoreCase(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 检测返回结果集中是否包含 Dict 注解
     *
     * @param listRecords
     * @return
     */
    private Boolean checkHasDict(List<?> listRecords) {
        if (listRecords != null && listRecords.size() > 0) {
            for (Field field : getAllFields(listRecords.get(0))) {
                if (field.getAnnotation(Dict.class) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取类的所有属性，包括父类
     *
     * @param object
     * @return
     */
    private Field[] getAllFields(Object object) {
        Class<?> clazz = object.getClass();
        List<Field> listField = new ArrayList<>();
        while (clazz != null) {
            Field[] fieldArr = clazz.getDeclaredFields();
            for (Field field : fieldArr) {
                // 放开访问权限，避免 private/跨模块不可读
                field.setAccessible(true);
                listField.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[listField.size()];
        listField.toArray(fields);
        return fields;
    }

    /**
     * 字典字段分组键：表 + code 列 + text 列
     */
    private record DictGroup(String table, String code, String text) {
    }
}
