package com.xuanchen.auth.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 密码
     */
    private String password;
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
     * 盐值
     */
    private String salt;
    /**
     * 删除状态（0正常，1已删除）
     */
    private Integer delFlag;
    /**
     * 验证码
     */
    @TableField(exist = false)
    private String captcha;
}
