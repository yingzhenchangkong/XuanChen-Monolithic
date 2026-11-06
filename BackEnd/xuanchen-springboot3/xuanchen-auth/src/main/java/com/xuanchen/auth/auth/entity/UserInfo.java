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
     * 是否要求下次登录修改密码（1是0否）：为 1 时前端跳转强制改密页，后端仅放行改密/登出等必要接口
     */
    private Integer pwdResetRequired;
}
