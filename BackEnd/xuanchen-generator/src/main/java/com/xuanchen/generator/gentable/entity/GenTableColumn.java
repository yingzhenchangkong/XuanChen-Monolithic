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
     * 列名称
     */
    @ExcelProperty(value = "列名称")
    private String columnName;
    /**
     * 列注释
     */
    @ExcelProperty(value = "列注释")
    private String columnComment;
    /**
     * 是否必填
     */
    @ExcelProperty(value = "是否必填")
    private Boolean isRequired;
    /**
     * 是否在编辑页面显示
     */
    @ExcelProperty(value = "是否在编辑页面显示")
    private Boolean isEdit;
    /**
     * 是否在列表页面显示
     */
    @ExcelProperty(value = "是否在列表页面显示")
    private Boolean isList;
    /**
     * 是否在查询页面显示
     */
    @ExcelProperty(value = "是否在查询页面显示")
    private Boolean isQuery;
    /**
     * 查询方式
     */
    @ExcelProperty(value = "查询方式")
    private String queryType;
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
