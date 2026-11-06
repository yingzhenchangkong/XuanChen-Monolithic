package com.xuanchen.common.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 导出字段值翻译注解。
 * <p>
 * 标注在实体的导出字段上，配合 {@link com.xuanchen.common.excel.converter.ExcelDictConverter}
 * 把数据库中的编码值（如 1/2）翻译为中文文案（如 正常/冻结）写入 Excel。
 * <p>
 * {@link #value()} 为翻译类型，解析顺序见
 * {@link com.xuanchen.common.excel.ExcelDictTranslator}：
 * <ol>
 *     <li>与 {@code sys_dict.dict_code} 同名时，取【字典管理】中配置的字典项（推荐，界面可维护）；</li>
 *     <li>业务模块注册的自定义 {@code ExcelDictResolver} Bean。</li>
 * </ol>
 *
 * @author XuanChen
 * @date 2026-09-28
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelDict {
    /**
     * 翻译类型：DB 字典编码（如 {@code "user_status"}、{@code "del_flag"}）
     * 或自定义解析器约定的类型名
     *
     * @return 翻译类型
     */
    String value();
}
