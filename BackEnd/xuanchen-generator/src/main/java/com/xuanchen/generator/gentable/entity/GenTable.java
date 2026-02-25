package com.xuanchen.generator.gentable.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 数据库表
 *
 * @author XuanChen
 * @date 2026-02-25
 */
@Data
public class GenTable implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 数据库ID
     */
    @ExcelProperty(value = "数据库ID")
    private String databaseId;
    /**
     * 数据库表名
     */
    @ExcelProperty(value = "数据库表名")
    private String tableName;
    /**
     * 数据库表注释
     */
    @ExcelProperty(value = "数据库表注释")
    private String tableComment;
    /**
     * 关联子表的表名
     */
    @ExcelProperty(value = "关联子表的表名")
    private String subTableName;
    /**
     * 子表关联的外键名
     */
    @ExcelProperty(value = "子表关联的外键名")
    private String subTableFkName;
    /**
     * 使用的模板
     */
    @ExcelProperty(value = "使用的模板")
    private String template;
    /**
     * 输出目录
     */
    @ExcelProperty(value = "输出目录")
    private String outputDir;
    /**
     * 包名
     */
    @ExcelProperty(value = "包名")
    private String packageName;
    /**
     * 模块名
     */
    @ExcelProperty(value = "模块名")
    private String moduleName;
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
