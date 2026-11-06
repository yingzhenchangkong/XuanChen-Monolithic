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
@TableName("sys_account")
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
     * 密码（入站仅接收登录明文；BCrypt 哈希等内部值禁止随任何响应返回）
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /**
     * 盐值（历史 SHA-512 方案使用；BCrypt 哈希账号为 null，盐内嵌于 $2a$ 哈希串）
     */
    private String salt;
    /**
     * 是否要求下次登录修改密码（1是0否）：新增/导入/管理员重置后置 1，本人改密成功后置 0
     */
    private Integer pwdResetRequired;
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
     * 验证码
     */
    @TableField(exist = false)
    private String captcha;
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
    /**
     * 记住我（登录时前端传入，控制是否签发 7 天刷新令牌，非数据库字段）
     */
    @TableField(exist = false)
    private Boolean rememberMe;
    /**
     * 昵称（对应 sys_account.nick_name：系统内展示名）
     */
    private String nickName;
    /**
     * 邮箱（对应 sys_account.email：找回密码/通知的账号凭证）
     */
    private String email;
}
