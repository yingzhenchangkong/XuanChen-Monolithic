package com.xuanchen.system.sysconfig.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->参数配置
 *
 * @author XuanChen
 * @date 2026-01-10
 */
public class SysConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 参数编码
     */
    @ExcelProperty("参数编码")
    @ColumnWidth(20)
    private String configCode;
    /**
     * 参数名称
     */
    @ExcelProperty("参数名称")
    @ColumnWidth(20)
    private String configName;
    /**
     * 参数描述
     */
    @ExcelProperty("参数描述")
    @ColumnWidth(40)
    private String configDescription;
    /**
     * 参数键名
     */
    @ExcelProperty("参数键名")
    @ColumnWidth(20)
    private String configKey;
    /**
     * 参数键值
     */
    @ExcelProperty("参数键值")
    @ColumnWidth(20)
    private String configValue;
    /**
     * 参数类型（1系统参数，2业务参数）
     */
    @ExcelProperty("参数类型")
    @ColumnWidth(20)
    private String configType;
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