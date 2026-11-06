package com.xuanchen.auth.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.mapper.AuthMapper;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.auth.utils.PasswordCodec;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.server.WebSocketServer;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service接口实现类-->认证、授权 通用
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Service
@RequiredArgsConstructor
public class AuthServiceCommonImpl implements IAuthServiceCommon {
    private final AuthMapper authMapper;
    private final PasswordCodec passwordCodec;
    private final RedisUtil redisUtil;

    private Auth getByUserName(String userName) {
        QueryWrapper<Auth> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        return authMapper.selectOne(queryWrapper);
    }

    @Override
    public String encryptPassword(String password) {
        return passwordCodec.encode(password);
    }

    @Override
    public String getUserNameByToken(String token) {
        return JwtUtil.getUsername(token);
    }

    @Override
    public Map<String, Object> getUserByUserName(String userName) {
        //使用 JOIN 查询同时加载账号字段 + 人员信息字段
        Map<String, Object> raw = authMapper.getUserWithProfile(userName);
        // 用户可能已被删除而其 Redis 会话仍在 TTL 内（在线用户扫描等场景），返回 null 由调用方跳过，避免 NPE 导致整页 500
        if (raw == null) {
            return null;
        }
        //归一化 Map 键为 camelCase：getUserWithProfile 的 SQL 别名已是 camelCase，
        //但不同 MyBatis 版本/驱动对 resultType=Map 的键映射行为不完全一致，
        //此处做一层防御，确保下游（在线用户列表 isAccountInvalid 等）始终能取到值
        Map<String, Object> userData = normalizeMapKeys(raw);
        Map<String, Object> map = new HashMap<>();
        map.put("id", userData.get("id"));
        map.put("userName", userData.get("userName"));
        map.put("nickName", userData.get("nickName"));
        map.put("mobile", userData.get("mobile"));
        map.put("email", userData.get("email"));
        map.put("avatar", userData.get("avatar"));
        //供在线用户列表判断账号是否已删除/冻结（失效会话不应展示）。
        //必须与 sb3 保持一致：OnlineUserInfoServiceImpl.isAccountInvalid 约定
        //delFlag/status 为空即视为失效，若此处不返回，在线用户扫描会把全部有效会话
        //误判为失效并逐个回收（含调用者自身会话），导致列表为空且刷新后被踢回登录页
        map.put("delFlag", userData.get("delFlag"));
        map.put("status", userData.get("status"));
        return map;
    }

    @Override
    public boolean checkPassword(String userName, String rawPassword) {
        if (StringUtil.isEmpty(userName) || StringUtil.isEmpty(rawPassword)) {
            return false;
        }
        Auth sysUser = getByUserName(userName);
        if (sysUser == null) {
            return false;
        }
        return passwordCodec.matches(rawPassword, sysUser.getPassword());
    }

    @Override
    public void revokeRefreshTokens(String userName) {
        if (StringUtil.isEmpty(userName)) {
            return;
        }
        // 刷新令牌键前缀与用户令牌键前缀不同（PREFIX_REFRESH_TOKEN_ vs PREFIX_USER_TOKEN_），
        // 不会被在线用户扫描误命中；此处按前缀 SCAN 后以值中的 username 精确匹配归属
        Set<String> refreshKeys = redisUtil.scanKeys(AuthConst.PREFIX_REFRESH_TOKEN + "*");
        if (refreshKeys == null || refreshKeys.isEmpty()) {
            return;
        }
        for (String refreshKey : refreshKeys) {
            Object value = redisUtil.get(refreshKey);
            // 值为空（SCAN 与 GET 间隙已过期）：一并清理，避免残留无效凭据
            if (value == null) {
                redisUtil.del(refreshKey);
                continue;
            }
            if (userName.equals(refreshTokenOwner(value.toString()))) {
                redisUtil.del(refreshKey);
            }
        }
    }

    @Override
    public void revokeRefreshTokensBySession(String userName, String sessionId) {
        if (StringUtil.isEmpty(sessionId)) {
            return;
        }
        Set<String> refreshKeys = redisUtil.scanKeys(AuthConst.PREFIX_REFRESH_TOKEN + "*");
        if (refreshKeys == null || refreshKeys.isEmpty()) {
            return;
        }
        for (String refreshKey : refreshKeys) {
            Object value = redisUtil.get(refreshKey);
            if (value == null) {
                redisUtil.del(refreshKey);
                continue;
            }
            String raw = value.toString();
            if (!sessionId.equals(refreshTokenSessionId(raw))) {
                continue;
            }
            // sessionId 为 UUID，理论上全局唯一；仍以 username 复核，避免脏数据误删他人凭据
            String owner = refreshTokenOwner(raw);
            if (StringUtil.isEmpty(userName) || userName.equals(owner)) {
                redisUtil.del(refreshKey);
            }
        }
    }

    /**
     * 解析刷新令牌值中的归属用户名。
     * 新结构为 JSON {"username":..,"sessionId":..}；历史结构为纯用户名，此处兼容旧值。
     *
     * @return 归属用户名；无法解析返回 null
     */
    private String refreshTokenOwner(String rawValue) {
        if (StringUtil.isEmpty(rawValue)) {
            return null;
        }
        if (!rawValue.startsWith("{")) {
            // 历史结构：值即 username
            return rawValue;
        }
        try {
            Object owner = JsonUtils.parseToMap(rawValue).get("username");
            return owner == null ? null : owner.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 解析刷新令牌值中绑定的会话ID（历史结构无此字段，返回 null）
     */
    private String refreshTokenSessionId(String rawValue) {
        if (StringUtil.isEmpty(rawValue) || !rawValue.startsWith("{")) {
            return null;
        }
        try {
            Object sessionId = JsonUtils.parseToMap(rawValue).get("sessionId");
            return sessionId == null ? null : sessionId.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 读取某条会话（访问令牌）元数据中记录的会话ID
     */
    @Override
    public String getSessionIdByToken(String token) {
        if (StringUtil.isEmpty(token)) {
            return null;
        }
        Object metaObj = redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
        if (metaObj instanceof String meta && meta.startsWith("{")) {
            try {
                Object sessionId = JsonUtils.parseToMap(meta).get("sessionId");
                return sessionId == null ? null : sessionId.toString();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 撤销"被同 IP 新登录顶掉的旧会话"的刷新令牌。
     * <p>
     * 若不做这一步，旧终端仍持有 httpOnly 刷新 Cookie，下一次请求 401 即可静默复活，
     * 使"同 IP 重复登录顶掉旧会话"形同虚设。
     *
     * @param replacedToken 被顶掉的旧访问令牌
     * @param newSessionId  新会话的会话ID；与旧会话同属一条会话（续期场景）时不撤销，
     *                      避免误伤正在使用的那一枚刷新令牌
     */
    private void revokeReplacedSessionRefreshToken(String userName, String replacedToken, String newSessionId) {
        String replacedSessionId = getSessionIdByToken(replacedToken);
        if (StringUtil.isEmpty(replacedSessionId) || replacedSessionId.equals(newSessionId)) {
            // 无会话ID（历史数据）或新旧同属一条会话：不撤销
            return;
        }
        revokeRefreshTokensBySession(userName, replacedSessionId);
    }

    @Override
    public void kickOutOtherSessions(String userName, String keepSessionId, String reason) {
        if (StringUtil.isEmpty(userName)) {
            return;
        }
        for (KickedSession kicked : removeUserSessions(userName, keepSessionId)) {
            if (StringUtil.isEmpty(kicked.sessionId())) {
                continue;
            }
            // 按会话精确撤销"记住我"刷新令牌：被踢终端下次请求 401 时无法再静默换发新令牌
            revokeRefreshTokensBySession(userName, kicked.sessionId());
            // 立即断开被踢会话的长连接：会话已失效，长连接不能继续收到服务端定向推送
            WebSocketServer.closeSessionOnKick(kicked.sessionId(), kicked.token(), reason);
        }
        // 兜底：无法识别"当前会话"（历史数据无会话ID）时撤销该账号全部刷新令牌。
        // 此时当前会话也已被删除，若仍保留刷新凭据，浏览器可凭它换发新访问令牌，
        // 使"改密踢出旧会话"形同虚设
        if (StringUtil.isEmpty(keepSessionId)) {
            revokeRefreshTokens(userName);
        }
    }

    @Override
    public void kickOutAccount(String userName, String reason) {
        if (StringUtil.isEmpty(userName)) {
            return;
        }
        // 1、撤销该账号全部"记住我"刷新令牌（兼容历史纯用户名结构），
        //    否则被停用/删除的账号仍可凭刷新 Cookie 静默换发新访问令牌
        revokeRefreshTokens(userName);
        // 2、断开该账号全部 WebSocket 长连接：账号已失效，长连接必须即时断开，
        //    而不是等到下一次 HTTP 请求 401 才被客户端感知
        WebSocketServer.closeUserOnKick(userName, reason);
        // 3、删除该账号全部在线会话（访问令牌 + 同 IP 索引）
        removeUserSessions(userName, null);
    }

    /**
     * 删除指定用户的在线会话（访问令牌 + 同 IP 索引），可选择保留其中一条。
     * <p>
     * 登录时按 PREFIX_USER_BY_IP + 用户名_IP 建立了到 token 的索引，此处先按前缀扫描候选索引，
     * 再以 token 元数据中的 username 精确确认，避免 admin / admin_backup 这类用户名前缀碰撞误删他人会话。
     *
     * @param userName      用户名
     * @param keepSessionId 需要保留的会话ID（本人改密时的当前会话）；为空则删除全部会话
     * @return 被删除的会话列表，供调用方按会话撤销刷新令牌、断开 WebSocket
     */
    private List<KickedSession> removeUserSessions(String userName, String keepSessionId) {
        List<KickedSession> removed = new ArrayList<>();
        Set<String> ipIndexKeys = redisUtil.scanKeys(AuthConst.PREFIX_USER_BY_IP + userName + "_*");
        if (ipIndexKeys == null || ipIndexKeys.isEmpty()) {
            return removed;
        }
        for (String indexKey : ipIndexKeys) {
            Object tokenObj = redisUtil.get(indexKey);
            if (tokenObj == null) {
                redisUtil.del(indexKey);
                continue;
            }
            String token = tokenObj.toString();
            Object metaObj = redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
            String sessionId = null;
            boolean belongsToUser;
            if (metaObj instanceof String meta && meta.startsWith("{")) {
                try {
                    Map<String, Object> metaMap = JsonUtils.parseToMap(meta);
                    belongsToUser = userName.equals(metaMap.get("username"));
                    sessionId = metaMap.get("sessionId") == null ? null : metaMap.get("sessionId").toString();
                } catch (Exception ignored) {
                    belongsToUser = false;
                }
            } else {
                // 兼容旧结构（值即 token、无元数据）：键前缀匹配即视为该用户会话
                belongsToUser = true;
            }
            if (!belongsToUser) {
                continue;
            }
            // 本人改密：保留当前客户端会话，使其"记住我"凭据与长连接继续有效
            if (StringUtil.isNotEmpty(keepSessionId) && keepSessionId.equals(sessionId)) {
                continue;
            }
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
            redisUtil.del(indexKey);
            removed.add(new KickedSession(sessionId, token));
        }
        return removed;
    }

    /**
     * 被踢会话标识：会话ID（历史会话无元数据时为 null）与访问令牌
     */
    private record KickedSession(String sessionId, String token) {
    }

    /**
     * 归一化 Map 键为 camelCase。
     * getUserWithProfile 等 XML 查询的 SQL 别名已是 camelCase（如 delFlag、pwdResetRequired），
     * 但不同 MyBatis 版本/驱动对 resultType=Map 的键映射行为不完全一致：
     * 有的直接返回 SQL 别名，有的会再做一次 snake_case→camelCase 转换。
     * 此方法同时尝试两种格式，确保下游代码始终能取到值。
     */
    private Map<String, Object> normalizeMapKeys(Map<String, Object> raw) {
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            normalized.put(key, entry.getValue());
            if (key.contains("_")) {
                normalized.put(snakeToCamel(key), entry.getValue());
            }
        }
        return normalized;
    }

    private static String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder(snake.length());
        boolean upper = false;
        for (int i = 0; i < snake.length(); i++) {
            char c = snake.charAt(i);
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> issueLoginSession(String userName, String clientIp, String deviceType) {
        return issueLoginSession(userName, clientIp, deviceType, null);
    }

    @Override
    public Map<String, Object> issueLoginSession(String userName, String clientIp, String deviceType, String sessionId) {
        if (StringUtil.isEmpty(userName)) {
            return null;
        }
        //使用 JOIN 查询同时加载账号字段 + 人员信息字段
        Map<String, Object> raw = authMapper.getUserWithProfile(userName);
        if (raw == null) {
            return null;
        }
        Map<String, Object> userData = normalizeMapKeys(raw);
        String token = JwtUtil.sign(userName);
        // 会话ID：与会话/终端绑定，贯穿该会话的多次访问令牌轮换（续期时由调用方回传复用），
        // 作为"记住我"刷新令牌的绑定键，使强制退出能精确定位到单条会话
        String currentSessionId = StringUtil.isNotEmpty(sessionId)
                ? sessionId : UUID.randomUUID().toString().replace("-", "");

        // ========== 同一IP 重复登录处理（与 /login 逻辑保持一致） ==========
        String ipIndexKey = AuthConst.PREFIX_USER_BY_IP + userName + "_" + clientIp;
        Object oldToken = redisUtil.get(ipIndexKey);
        if (oldToken != null && !String.valueOf(oldToken).isEmpty()
                && !String.valueOf(oldToken).equals(token)) {
            // 被顶掉的旧会话：其刷新令牌必须一并失效，否则旧终端可凭刷新 Cookie 静默复活
            revokeReplacedSessionRefreshToken(userName, String.valueOf(oldToken), currentSessionId);
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + oldToken);
        }
        // ==================================================================

        // 写入 token 元数据（JSON）：包含用户名、IP、终端类型、会话ID、登录时间
        Map<String, Object> tokenMeta = new HashMap<>();
        tokenMeta.put("username", userName);
        tokenMeta.put("token", token);
        tokenMeta.put("ip", clientIp);
        tokenMeta.put("deviceType", deviceType);
        tokenMeta.put("sessionId", currentSessionId);
        tokenMeta.put("loginTime", System.currentTimeMillis());

        // Redis 会话 TTL 与 JWT 有效期一致，避免"幽灵会话"
        long ttlSeconds = JwtUtil.EXPIRE_SECONDS;
        redisUtil.set(AuthConst.PREFIX_USER_TOKEN + token, JsonUtils.toJson(tokenMeta), ttlSeconds);
        redisUtil.set(ipIndexKey, token, ttlSeconds);

        // 登录失败计数不在成功登录时清零：计数由固定窗口 TTL（LOGIN_FAIL_WINDOW_SECONDS）自然过期。
        // 若一次成功登录就清空，攻击者可在每次尝试中穿插一次合法登录（或获知一次正确口令后）
        // 重置计数，使基于失败次数的临时锁定失效；保留计数不影响已成功登录的会话。

        // 返回与登录响应一致的用户信息结构
        Map<String, Object> data = new HashMap<>();
        data.put("id", userData.get("id"));
        data.put("userName", userName);
        data.put("nickName", userData.get("nickName"));
        data.put("mobile", userData.get("mobile"));
        data.put("email", userData.get("email"));
        data.put("avatar", userData.get("avatar"));
        data.put("token", token);
        // 以数据库最新标志位为准（改密成功时已置 0）
        Object pwdResetRequiredObj = userData.get("pwdResetRequired");
        int pwdResetRequired = 0;
        if (pwdResetRequiredObj instanceof Number) {
            pwdResetRequired = ((Number) pwdResetRequiredObj).intValue();
        }
        data.put("pwdResetRequired", Integer.valueOf(1).equals(pwdResetRequired) ? 1 : 0);
        return data;
    }
}
