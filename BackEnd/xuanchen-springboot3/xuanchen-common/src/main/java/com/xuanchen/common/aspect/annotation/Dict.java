package com.xuanchen.common.aspect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字典注解
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dict {
    /**
     * 数据code
     *
     * @return String
     */
    String dicCode();

    /**
     * 数据Text
     *
     * @return String
     */
    String dicText() default "";

    /**
     * 数据字典表
     *
     * @return String
     */
    String dictTable() default "";
}
