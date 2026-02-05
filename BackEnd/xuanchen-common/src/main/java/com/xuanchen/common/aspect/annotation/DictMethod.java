package com.xuanchen.common.aspect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通过此注解声明的方法，自动实现字典翻译
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DictMethod {
    String value() default "";
}
