package com.xuanchen.auth.auth.service.impl;

import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.service.IAuthServiceCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service接口实现类-->认证、授权 通用
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Service
public class AuthServiceCommonImpl implements IAuthServiceCommon {
    @Autowired
    private IAuthService authService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public String getUserNameByToken(String token) {
        return JwtUtil.getUsername(token);
    }

    @Override
    public Map<String, Object> getUserByUserName(String userName) {
        Auth sysUser = authService.getByUserName(userName);
        Map<String, Object> map = new HashMap<>();
        map.put("id", sysUser.getId());
        map.put("userName", sysUser.getUserName());
        map.put("nickName", sysUser.getNickName());
        map.put("mobile", sysUser.getMobile());
        map.put("email", sysUser.getEmail());
        map.put("avatar", sysUser.getAvatar());
        return map;
    }
}
