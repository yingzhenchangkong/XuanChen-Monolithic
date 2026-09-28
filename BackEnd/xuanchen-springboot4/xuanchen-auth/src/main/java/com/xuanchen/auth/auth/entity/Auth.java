package com.xuanchen.auth.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
@Data
@TableName("sys_user")
public class Auth implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private String id;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 密码（入站仅接收登录明文；BCrypt 哈希等内部值禁止随任何响应返回）
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /**
     * 是否要求下次登录修改密码（1是 0否）
     */
    private Integer pwdResetRequired;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 账号状态（1正常，2冻结）
     */
    private Integer status;
    /**
     * 删除状态（0正常，1已删除）
     * 逻辑删除字段：MyBatis-Plus 对该实体的所有自动查询都会追加 del_flag = 0，
     * 避免各处手写 del_flag 条件遗漏导致已删除账号仍可登录/改密/被查询
     */
    @TableLogic
    private Integer delFlag;
    /**
     * 验证码id
     */
    @TableField(exist = false)
    private String captchaId;
    /**
     * 验证码token
     */
    @TableField(exist = false)
    private String captchaToken;
    /**
     * 邮箱验证码（找回密码第 2 步使用，非数据库字段）
     */
    @TableField(exist = false)
    private String emailCode;
    /**
     * 一次性密码重置令牌（找回密码第 3 步使用，非数据库字段）
     */
    @TableField(exist = false)
    private String resetToken;
}
