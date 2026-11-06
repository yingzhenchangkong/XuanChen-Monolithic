package com.xuanchen.generator.generator.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 字段视图 --> 代码生成模板数据（一列 = 一个视图对象）
 * <p>
 * 由 gen_table_column 的界面配置（或未配置时按物理元数据推断）转换而来，
 * 经 beforeOutputFile 钩子注入 Freemarker 数据模型，供 Index.vue / Operation.vue /
 * Controller 模板按字段循环渲染。控件类型取值与字典 ui_control_type 对齐：
 * Checkbox / DatePicker / Input / InputNumber / Radio / Select / Switch /
 * TimePicker / TreeSelect / Upload。
 *
 * @author XuanChen
 * @date 2026-10-18
 */
@Data
public class GenFieldView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 数据库列名（下划线，如 user_name） */
    private String columnName;
    /** Java 属性名（小驼峰，如 userName） */
    private String property;
    /** 字段注释（用作界面标签） */
    private String comment;
    /** Java 类型简名（String / Integer / Long / BigDecimal / Boolean / LocalDate / LocalDateTime / LocalTime） */
    private String javaType;

    /** 是否查询字段 */
    private Boolean isQuery;
    /** 查询控件类型（ui_control_type 字典值） */
    private String queryType;
    /** 查询匹配方式：like / eq */
    private String queryOp;

    /** 是否编辑字段 */
    private Boolean isEdit;
    /** 是否必填 */
    private Boolean isRequired;
    /** 编辑控件类型（ui_control_type 字典值） */
    private String editType;

    /** 是否列表字段 */
    private Boolean isList;
    /** 字典编码（选择类控件；为空时模板生成 TODO_列名 占位） */
    private String dictCode;
}
