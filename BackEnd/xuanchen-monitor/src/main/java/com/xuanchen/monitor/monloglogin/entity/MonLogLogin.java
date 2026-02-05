package com.xuanchen.monitor.monloglogin.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xuanchen.common.aspect.annotation.Dict;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->日志 登录
 *
 * @author XuanChen
 * @date 2026-02-04
 */
@Data
public class MonLogLogin implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;

    /**
     * 用户名
     */
    @ExcelProperty("用户名")
    @ColumnWidth(20)
    @Dict(dictTable = "sys_user", dicCode = "user_name", dicText = "nick_name")
    private String userName;

    /**
     * 登录时间
     */
    @ExcelProperty("登录时间")
    @ColumnWidth(20)
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /**
     * 登录IP地址
     */
    @ExcelProperty("登录IP地址")
    @ColumnWidth(20)
    private String ipAddress;

    /**
     * 登录状态（true: 成功, false: 失败）
     */
    @ExcelProperty("登录状态")
    @ColumnWidth(20)
    private Boolean status;

    /**
     * 描述信息
     */
    @ExcelProperty("描述信息")
    @ColumnWidth(20)
    private String description;
}