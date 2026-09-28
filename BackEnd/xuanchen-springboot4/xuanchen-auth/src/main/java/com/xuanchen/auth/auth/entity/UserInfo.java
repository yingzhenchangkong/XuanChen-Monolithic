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
    /**
     * 是否要求立即修改密码（1：使用随机临时密码/被管理员重置后首次登录；0：否）。
     * 为 1 时服务端仅允许调用修改密码/登出接口，前端应引导至改密页。
     */
    private Integer pwdResetRequired;
}
