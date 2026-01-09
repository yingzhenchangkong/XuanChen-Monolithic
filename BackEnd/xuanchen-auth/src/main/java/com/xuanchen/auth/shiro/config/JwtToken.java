package com.xuanchen.auth.shiro.config;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * shiro可用的token
 *
 * @author XuanChen
 * @date 2025-03-12
 */
public class JwtToken implements AuthenticationToken {
    private final String token;

    public JwtToken(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
