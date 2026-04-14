package com.xuanchen.auth.auth.entity;

import lombok.Data;

/**
 * 实体类-->用户信息
 *
 * @author XuanChen
 * @date 2025-03-29
 */
@Data
public class UserInfo {
    /**
     * ID
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
     * 手机号
     */
    private String mobile;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 令牌
     */
    private String token;
}
