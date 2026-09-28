package com.xuanchen.auth.auth.service.impl;

import com.alibaba.fastjson2.JSON;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service接口实现类-->认证、授权 通用
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Service
@RequiredArgsConstructor
public class AuthServiceCommonImpl implements IAuthServiceCommon {
    private final IAuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;

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
        // 用户可能已被删除而其 Redis 会话仍在 TTL 内（在线用户扫描等场景），返回 null 由调用方跳过，避免 NPE 导致整页 500
        if (sysUser == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", sysUser.getId());
        map.put("userName", sysUser.getUserName());
        map.put("nickName", sysUser.getNickName());
        map.put("mobile", sysUser.getMobile());
        map.put("email", sysUser.getEmail());
        map.put("avatar", sysUser.getAvatar());
        return map;
    }

    @Override
    public boolean checkPassword(String userName, String rawPassword) {
        if (StringUtil.isEmpty(userName) || StringUtil.isEmpty(rawPassword)) {
            return false;
        }
        Auth sysUser = authService.getByUserName(userName);
        if (sysUser == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, sysUser.getPassword());
    }

    @Override
    public void kickOutUser(String userName) {
        if (StringUtil.isEmpty(userName)) {
            return;
        }
        // 登录时按 PREFIX_USER_BY_IP + 用户名_IP 建立了到 token 的索引，
        // 先按前缀扫描候选索引，再以 token 元数据中的 username 精确确认，
        // 避免 admin / admin_backup 这类用户名前缀碰撞误删他人会话
        Set<String> ipIndexKeys = redisUtil.scanKeys(AuthConst.PREFIX_USER_BY_IP + userName + "_*");
        if (ipIndexKeys == null || ipIndexKeys.isEmpty()) {
            return;
        }
        for (String indexKey : ipIndexKeys) {
            Object token = redisUtil.get(indexKey);
            if (token == null) {
                redisUtil.del(indexKey);
                continue;
            }
            Object metaObj = redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
            boolean belongsToUser = false;
            if (metaObj instanceof String meta && meta.startsWith("{")) {
                try {
                    belongsToUser = userName.equals(JSON.parseObject(meta).getString("username"));
                } catch (Exception ignored) {
                    belongsToUser = false;
                }
            } else {
                // 兼容旧结构（值即 token、无元数据）：键前缀匹配即视为该用户会话
                belongsToUser = true;
            }
            if (belongsToUser) {
                redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
                redisUtil.del(indexKey);
            }
        }
    }

    @Override
    public Map<String, Object> issueLoginSession(String userName, String clientIp, String deviceType) {
        if (StringUtil.isEmpty(userName)) {
            return null;
        }
        Auth sysUser = authService.getByUserName(userName);
        if (sysUser == null) {
            return null;
        }
        String token = JwtUtil.sign(userName);

        // ========== 同一IP 重复登录处理（与 /login 逻辑保持一致） ==========
        String ipIndexKey = AuthConst.PREFIX_USER_BY_IP + userName + "_" + clientIp;
        Object oldToken = redisUtil.get(ipIndexKey);
        if (oldToken != null && !String.valueOf(oldToken).isEmpty()
                && !String.valueOf(oldToken).equals(token)) {
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + oldToken);
        }
        // ==================================================================

        // 写入 token 元数据（JSON）：包含用户名、IP、终端类型、登录时间
        Map<String, Object> tokenMeta = new HashMap<>();
        tokenMeta.put("username", userName);
        tokenMeta.put("token", token);
        tokenMeta.put("ip", clientIp);
        tokenMeta.put("deviceType", deviceType);
        tokenMeta.put("loginTime", System.currentTimeMillis());

        // Redis 会话 TTL 与 JWT 有效期一致，避免"幽灵会话"
        long ttlSeconds = JwtUtil.EXPIRE_SECONDS;
        redisUtil.set(AuthConst.PREFIX_USER_TOKEN + token, JSON.toJSONString(tokenMeta), ttlSeconds);
        redisUtil.set(ipIndexKey, token, ttlSeconds);

        // 登录失败计数不在成功登录时清零：计数由固定窗口 TTL（LOGIN_FAIL_WINDOW_SECONDS）自然过期。
        // 若一次成功登录就清空，攻击者可在每次尝试中穿插一次合法登录（或获知一次正确口令后）
        // 重置计数，使基于失败次数的临时锁定失效；保留计数不影响已成功登录的会话。

        // 返回与登录响应一致的用户信息结构
        Map<String, Object> data = new HashMap<>();
        data.put("id", sysUser.getId());
        data.put("userName", userName);
        data.put("nickName", sysUser.getNickName());
        data.put("mobile", sysUser.getMobile());
        data.put("email", sysUser.getEmail());
        data.put("avatar", sysUser.getAvatar());
        data.put("token", token);
        // 以数据库最新标志位为准（改密成功时已置 0）
        data.put("pwdResetRequired", Integer.valueOf(1).equals(sysUser.getPwdResetRequired()) ? 1 : 0);
        return data;
    }
}
