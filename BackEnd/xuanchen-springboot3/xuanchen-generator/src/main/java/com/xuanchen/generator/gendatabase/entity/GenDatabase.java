package com.xuanchen.generator.gendatabase.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 数据库
 *
 * @author XuanChen
 * @date 2026-02-18
 */
@Data
public class GenDatabase implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 连接类型
     */
    @ExcelProperty("连接类型")
    @ColumnWidth(15)
    private String connType;
    /**
     * 连接名称
     */
    @ExcelProperty("连接名称")
    @ColumnWidth(15)
    private String connName;
    /**
     * 主机
     */
    @ExcelProperty("主机")
    @ColumnWidth(15)
    private String host;
    /**
     * 端口
     */
    @ExcelProperty("端口")
    @ColumnWidth(15)
    private String port;
    /**
     * 数据库名称
     */
    @ExcelProperty("数据库名称")
    @ColumnWidth(15)
    private String dbName;
    /**
     * 用户名
     */
    @ExcelProperty("用户名")
    @ColumnWidth(15)
    private String userName;
    /**
     * 密码
     */
    @ExcelProperty("密码")
    @ColumnWidth(15)
    private String password;
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
