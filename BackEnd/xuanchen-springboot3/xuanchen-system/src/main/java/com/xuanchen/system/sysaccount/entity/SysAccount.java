package com.xuanchen.system.sysaccount.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->用户账号（登录、认证、权限）
 * 一个账号可选绑定一个人员档案（person_id -> md_person.id），人员可对应多个账号
 *
 * @author XuanChen
 * @date 2026-10-16
 */
@Data
@TableName("sys_account")
public class SysAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private String id;
    /**
     * 人员ID（关联 md_person.id，可为空=无人员信息的纯账号）
     */
    private String personId;
    /**
     * 用户名（登录名）
     */
    private String userName;
    /**
     * 昵称（系统内展示名：下拉、通知、在线用户、操作日志等统一使用）
     */
    private String nickName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 手机号（登录凭证 / 短信验证 / 通知）
     */
    private String mobile;
    /**
     * 邮箱（找回密码 / 通知）
     */
    private String email;
    /**
     * 密码
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /**
     * 是否要求下次登录修改密码（1是 0否）
     */
    private Integer pwdResetRequired;
    /**
     * 账号状态（1正常，2冻结）
     */
    private Integer status;
    /**
     * 盐值
     */
    @JsonIgnore
    private String salt;
    /**
     * 排序码
     */
    private Integer orderNo;
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
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
