package com.xuanchen.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 登录主体：在 Spring Security 默认 User 之上携带用户ID与"是否要求修改密码"标志，
 * 供过滤器做强制改密访问控制，避免每个请求再查库。
 *
 * @author XuanChen
 * @date 2026-09-19
 */
public class LoginUser extends User {
    private final String userId;
    private final boolean pwdResetRequired;

    public LoginUser(String userId, String username, String password, boolean enabled,
                     boolean pwdResetRequired,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.pwdResetRequired = pwdResetRequired;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isPwdResetRequired() {
        return pwdResetRequired;
    }
}
