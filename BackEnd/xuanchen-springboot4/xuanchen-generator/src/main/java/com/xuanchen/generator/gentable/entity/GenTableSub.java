package com.xuanchen.generator.gentable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实体类 --> 代码生成子表配置（一个主表配置可挂多个子表）
 *
 * @author XuanChen
 * @date 2026-10-20
 */
@Data
@TableName("gen_table_sub")
public class GenTableSub implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 主表配置ID（gen_table.id）
     */
    @ExcelIgnore
    private String tableId;
    /**
     * 子表 物理表名
     */
    @ExcelProperty(value = "子表名")
    private String subTableName;
    /**
     * 子表 关联主表的外键列
     */
    @ExcelProperty(value = "子表外键")
    private String subFkName;
    /**
     * 子表标题（页面展示用；为空时取物理表注释）
     */
    @ExcelProperty(value = "子表标题")
    private String subComment;
    /**
     * 排序码（决定页面中子表的展示顺序）
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

    /**
     * 该子表的字段配置（非数据库列，保存/回显时随子表一起传输）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private List<GenTableColumn> columns;
}
