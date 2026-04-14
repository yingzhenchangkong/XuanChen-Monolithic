package com.xuanchen.monitor.monlogoperation.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xuanchen.common.aspect.annotation.Dict;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->操作日志
 *
 * @author XuanChen
 * @date 2026-02-07
 */
@Data
public class MonLogOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelIgnore
    private String id;

    /**
     * 操作用户
     */
    @ExcelProperty("操作用户")
    @ColumnWidth(20)
    @Dict(dictTable = "sys_user", dicCode = "user_name", dicText = "nick_name")
    private String userName;
    /**
     * 模块
     */
    @ExcelProperty("模块")
    @ColumnWidth(20)
    private String module;

    /**
     * 操作时间
     */
    @ExcelProperty("操作时间")
    @ColumnWidth(20)
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;
    /**
     * 时间区间查询
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] timeRange;

    /**
     * 开始时间（用于区间查询）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String beginTime;

    /**
     * 结束时间（用于区间查询）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String endTime;

    /**
     * 操作类型（如：新增、修改、删除等）
     */
    @ExcelProperty("操作类型")
    @ColumnWidth(20)
    private String operationType;

    /**
     * 操作描述
     */
    @ExcelProperty("操作描述")
    @ColumnWidth(30)
    private String description;

    /**
     * 请求URL
     */
    @ExcelProperty("请求URL")
    @ColumnWidth(30)
    private String requestUrl;

    /**
     * 请求参数
     */
    @ExcelProperty("请求参数")
    @ColumnWidth(40)
    private String requestParams;

    /**
     * IP地址
     */
    @ExcelProperty("IP地址")
    @ColumnWidth(20)
    private String ipAddress;

    /**
     * 状态（成功/失败）
     */
    @ExcelProperty("状态")
    @ColumnWidth(15)
    private Boolean status;
}
