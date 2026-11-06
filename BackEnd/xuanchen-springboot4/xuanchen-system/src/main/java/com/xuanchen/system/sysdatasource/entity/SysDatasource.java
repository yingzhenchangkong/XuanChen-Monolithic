package com.xuanchen.system.sysdatasource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->统一数据源（sys_datasource）：报表/API/大屏/代码生成共用。
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Data
@TableName("sys_datasource")
public class SysDatasource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源类型（mysql/oracle/sqlserver/postgresql）
     */
    private String dsType;

    /**
     * JDBC 连接串
     */
    private String jdbcUrl;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（AES 加密存储；返回前端时脱敏为空串）
     */
    private String password;

    /**
     * 状态（1启用 0停用）
     */
    private Integer status;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;

    /**
     * 是否已配置密码（列表脱敏展示用）
     */
    @TableField(exist = false)
    private Boolean hasPassword;
}
