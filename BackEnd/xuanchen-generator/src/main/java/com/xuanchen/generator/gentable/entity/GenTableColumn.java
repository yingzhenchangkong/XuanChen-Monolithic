package com.xuanchen.generator.gentable.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 数据库表字段
 *
 * @author XuanChen
 * @date 2026-02-25
 */
@Data
public class GenTableColumn implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 表id
     */
    @ExcelProperty(value = "表id")
    private String tableId;
    /**
     * 字段 名称
     */
    @ExcelProperty(value = "字段名称")
    private String columnName;
    /**
     * 字段 注释
     */
    @ExcelProperty(value = "字段注释")
    private String columnComment;
    /**
     * 字段 类型
     */
    @ExcelProperty(value = "字段类型")
    private String columnType;
    /**
     * 字段 长度
     */
    @ExcelProperty(value = "字段长度")
    private Integer columnLength;
    /**
     * 字段 小数点
     */
    @ExcelProperty(value = "字段 小数点")
    private Integer columnScale;
    /**
     * 字段 默认值
     */
    @ExcelProperty(value = "字段 默认值")
    private String columnDefault;
    /**
     * 字段 是否主键
     */
    @ExcelProperty(value = "字段 是否主键")
    private Boolean columnIsPk;
    /**
     * 字段 是否允许为NULL
     */
    @ExcelProperty(value = "字段 是否允许为NULL")
    private Boolean columnIsNullable;
    /**
     * 界面 是否查询字段
     */
    @ExcelProperty(value = "界面 是否查询字段")
    private Boolean uiIsQuery;
    /**
     * 界面 查询方式
     */
    @ExcelProperty(value = "界面 查询方式")
    private String uiQueryType;
    /**
     * 界面 是否编辑字段
     */
    @ExcelProperty(value = "界面 是否编辑字段")
    private Boolean uiIsEdit;
    /**
     * 界面 是否必填字段
     */
    @ExcelProperty(value = "界面 是否必填字段")
    private Boolean uiIsRequired;
    /**
     * 界面 编辑方式
     */
    @ExcelProperty(value = "界面 编辑方式")
    private String uiEditType;
    /**
     * 界面 是否列表字段
     */
    @ExcelProperty(value = "界面 是否列表字段")
    private Boolean uiIsList;
    /**
     * 排序码
     */
    @ExcelIgnore
    private Integer orderNo;
    /**
     * 状态（1启用，0停用）
     */
    @ExcelIgnore
    private Boolean status;
    /**
     * 删除状态（0正常，1已删除）
     */
    @ExcelIgnore
    @TableLogic
    private Boolean delFlag;
    /**
     * 创建者
     */
    @ExcelIgnore
    private String createBy;
    /**
     * 创建时间
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 更新者
     */
    @ExcelIgnore
    private String updateBy;
    /**
     * 更新时间
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
