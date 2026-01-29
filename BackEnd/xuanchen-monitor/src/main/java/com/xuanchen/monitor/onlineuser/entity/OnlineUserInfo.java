package com.xuanchen.monitor.onlineuser.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->在线用户信息
 *
 * @author XuanChen
 * @date 2026-01-29
 */
@Data
public class OnlineUserInfo implements Serializable {
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
     * 手机
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
     * 令牌
     */
    private String token;
}
