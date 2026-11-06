package com.xuanchen.common.excel.converter;

import com.xuanchen.common.excel.ExcelDictTranslator;
import com.xuanchen.common.excel.annotation.ExcelDict;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用 Excel 导出值翻译转换器。
 * <p>
 * 一个转换器服务所有需要"编码 -> 中文文案"的导出字段：它从当前字段的
 * {@link ExcelDict} 注解读取翻译类型，再到 {@link ExcelDictTranslator} 查表。
 * 字段上未标注 {@link ExcelDict} 或值为 null 时按原值/空值处理，不影响导出。
 * <p>
 * 用法：
 * <pre>{@code
 * @ExcelProperty(value = "账号状态", converter = ExcelDictConverter.class)
 * @ExcelDict("user_status")
 * private Integer status;
 * }</pre>
 *
 * @author XuanChen
 * @date 2026-09-28
 */
public class ExcelDictConverter implements Converter<Object> {

    /**
     * 字段 -> 翻译类型 缓存，避免逐单元格重复反射读注解；
     * NULL_TYPE 表示该字段未标注 @ExcelDict，直接原样输出
     */
    private static final String NULL_TYPE = "\0NULL";
    private static final ConcurrentHashMap<Field, String> FIELD_TYPE_CACHE = new ConcurrentHashMap<>();

    @Override
    public WriteCellData<?> convertToExcelData(Object value, ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        Field field = contentProperty == null ? null : contentProperty.getField();
        String type = resolveType(field);
        if (NULL_TYPE.equals(type)) {
            return new WriteCellData<>(String.valueOf(value));
        }
        return new WriteCellData<>(ExcelDictTranslator.translate(type, value));
    }

    private String resolveType(Field field) {
        if (field == null) {
            return NULL_TYPE;
        }
        String cached = FIELD_TYPE_CACHE.get(field);
        if (cached != null) {
            return cached;
        }
        ExcelDict dict = field.getAnnotation(ExcelDict.class);
        String type = dict == null ? NULL_TYPE : dict.value();
        FIELD_TYPE_CACHE.put(field, type);
        return type;
    }
}
