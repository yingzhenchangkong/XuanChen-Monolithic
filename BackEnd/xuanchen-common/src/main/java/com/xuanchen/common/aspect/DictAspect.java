package com.xuanchen.common.aspect;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xuanchen.common.api.DictApi.service.IDictApiService;
import com.xuanchen.common.aspect.annotation.Dict;
import com.xuanchen.common.utils.StringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 字典AOP类
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Aspect
@Component
public class DictAspect {
    @Autowired
    private IDictApiService dictApiService;

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

    private Object parseDictText(Object result) {
        List<Object> listRecords = ((IPage) (((Map) result).get("data"))).getRecords();
        List<Object> listRecordsNew = new ArrayList<>();
        Boolean hasDict = checkHasDict(listRecords);
        if (!hasDict) {
            return result;
        }
        for (Object record : listRecords) {
            JSONObject item = JSONObject.from(record);
            for (Field field : getAllFields(record)) {
                String name = field.getName();
                if (StringUtil.isEmpty(field.getName())) {
                    continue;
                }
                if (field.getAnnotation(Dict.class) != null) {
                    String table = field.getAnnotation(Dict.class).dictTable();
                    String code = field.getAnnotation(Dict.class).dicCode();
                    String codeValue = item.getString(name);
                    String text = field.getAnnotation(Dict.class).dicText();
                    String textValue = dictApiService.translateFieldToString(table, code, codeValue, text);
                    item.put(name + "_dictText", textValue);
                }
            }
            listRecordsNew.add(item);
        }
        ((IPage) (((Map) result).get("data"))).setRecords(listRecordsNew);
        return result;
    }

    /**
     * 检测返回结果集中是否包含 Dict 注解
     *
     * @param listRecords
     * @return
     */
    private Boolean checkHasDict(List<Object> listRecords) {
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
            List<Field> listFieldTemp = Arrays.asList(fieldArr);
            listField.addAll(listFieldTemp);
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[listField.size()];
        listField.toArray(fields);
        return fields;
    }
}
