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
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AuthServiceCommonImpl implements IAuthServiceCommon {
    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private PasswordCodec passwordCodec;

    @Autowired
    private RedisUtil redisUtil;

    private Auth getByUserName(String userName) {
        QueryWrapper<Auth> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        return authMapper.selectOne(queryWrapper);
    }

    @Override
    public String encryptPassword(String rawPassword) {
        return passwordCodec.encode(rawPassword);
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
        return passwordCodec.matches(rawPassword, sysUser.getPassword(), sysUser.getSalt());
    }

    @Override
    public String getUserNameByToken(String token) {
        return JwtUtil.getUsername(token);
    }

    @Override
    public Map<String, Object> getUserByUserName(String userName) {
        //使用 JOIN 查询同时加载账号字段 + 人员信息字段
        Map<String, Object> raw = authMapper.getUserWithProfile(userName);
        //用户可能已被物理删除但其会话令牌仍残留在 Redis（删除用户不会联动清理会话），
        //此处必须判空返回 null，由调用方决定跳过/回收，禁止直接解引用引发 NPE
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
        //供在线用户列表判断账号是否已删除/冻结（失效会话不应展示）
        map.put("delFlag", userData.get("delFlag"));
        map.put("status", userData.get("status"));
        return map;
    }

    /**
     * 读取某条会话（访问令牌）元数据中记录的会话ID
     */
    @Override
    public String getSessionIdByToken(String token) {
        if (StringUtil.isEmpty(token)) {
            return null;
        }
        Object metaObj = redisUtil.get(AuthConst.PREFIX_ONLINE_META + token);
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

    @Override
    public void revokeRefreshTokens(String userName) {
        if (StringUtil.isEmpty(userName)) {
            return;
        }
        // 刷新令牌键前缀与用户令牌/在线会话元数据前缀不同（PREFIX_REFRESH_TOKEN_ vs
        // PREFIX_USER_TOKEN_ / PREFIX_ONLINE_META_），不会被在线用户扫描误命中；
        // 此处按前缀 SCAN 后以值中的 username 精确匹配归属
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
        // 兜底：无法识别"当前会话"（历史数据无会话ID）时撤销该账号全部刷新令牌，
        // 否则浏览器上那枚刷新 Cookie 仍可换发新访问令牌，"改密踢出旧会话"形同虚设
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
        // 3、删除该账号全部在线会话（访问令牌 + 在线会话元数据 + 同 IP 索引）
        removeUserSessions(userName, null);
    }

    /**
     * 删除指定用户的在线会话（访问令牌 + 在线会话元数据 + 同 IP 索引），可选择保留其中一条。
     * <p>
     * 登录时按 PREFIX_USER_BY_IP + 用户名_IP 建立了到 token 的索引，此处先按前缀扫描候选索引，
     * 再以会话元数据中的 userName 精确确认，避免 admin / admin_backup 这类用户名前缀碰撞误删他人会话。
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
            Object metaObj = redisUtil.get(AuthConst.PREFIX_ONLINE_META + token);
            String sessionId = null;
            boolean belongsToUser;
            if (metaObj instanceof String meta && meta.startsWith("{")) {
                try {
                    Map<String, Object> metaMap = JsonUtils.parseToMap(meta);
                    belongsToUser = userName.equals(metaMap.get("userName"));
                    sessionId = metaMap.get("sessionId") == null ? null : metaMap.get("sessionId").toString();
                } catch (Exception ignored) {
                    belongsToUser = false;
                }
            } else {
                // 兼容旧结构（无元数据）：键前缀匹配即视为该用户会话
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
            redisUtil.del(AuthConst.PREFIX_ONLINE_META + token);
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
    public String getCurrentUserName() {
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof Auth authUser && authUser.getUserName() != null) {
                return authUser.getUserName();
            }
        } catch (Exception ex) {
            //无 Shiro 线程上下文（未登录请求/异步线程）时返回空串，与历史行为保持一致
        }
        return "";
    }

    @Override
    public Map<String, Object> rotateSession(String userName, String clientIp, String deviceType, String revokeToken) {
        if (StringUtil.isEmpty(userName)) {
            return null;
        }
        //JWT 使用服务端配置密钥签发，与口令哈希无关；调用方应在更新口令提交后调用本方法
        //使用 JOIN 查询同时加载账号字段 + 人员信息字段
        Map<String, Object> raw = authMapper.getUserWithProfile(userName);
        if (raw == null) {
            return null;
        }
        Map<String, Object> userData = normalizeMapKeys(raw);

        //1、解析当前请求会话的会话ID：改密后沿用同一会话ID，使浏览器上那枚绑定它的
        //   "记住我"刷新令牌继续有效（用户并未退出登录，只是换了口令，被迫重登属体验倒退）
        String resolvedSessionId = StringUtil.isNotEmpty(revokeToken) ? getSessionIdByToken(revokeToken) : null;

        //2、作废当前请求携带的旧会话（令牌键/元数据/同IP索引），与 /logout 同一清理逻辑
        if (StringUtil.isNotEmpty(revokeToken)) {
            Object metaObj = redisUtil.get(AuthConst.PREFIX_ONLINE_META + revokeToken);
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + revokeToken);
            redisUtil.del(AuthConst.PREFIX_ONLINE_META + revokeToken);
            if (metaObj instanceof String metaStr && metaStr.startsWith("{")) {
                try {
                    Map<String, Object> meta = JsonUtils.parseToMap(metaStr);
                    String loginIp = (String) meta.get("ip");
                    if (!StringUtil.isEmpty(loginIp)) {
                        redisUtil.del(AuthConst.PREFIX_USER_BY_IP + userName + "_" + loginIp);
                    }
                } catch (Exception ignored) {
                    //元数据损坏不影响新会话签发
                }
            }
        }

        //3、踢掉该账号的其他会话（其他终端/其他 IP）：撤销其"记住我"刷新令牌、断开其长连接、
        //   删除其访问令牌与元数据；当前会话（resolvedSessionId）被保留。
        //   注意两点：① 这里**不能**改用 revokeRefreshTokens 账号级撤销，那会连本机刷新令牌一起废掉；
        //   ② resolvedSessionId 为空（历史会话无元数据）时，本方法内部会删除全部会话并撤销
        //      该账号全部刷新令牌（保守兜底），避免无法定位当前会话时留下可复活的旧凭据
        kickOutOtherSessions(userName, resolvedSessionId, "密码已修改，请重新登录");

        //4、用服务端配置密钥签发新 JWT 并重建 Redis 会话（与 AuthController 登录签发保持同一结构）
        String token = JwtUtil.sign(userName);
        long ttlSeconds = JwtUtil.EXPIRE_TIME * 2 / 1000;
        String ipIndexKey = AuthConst.PREFIX_USER_BY_IP + userName + "_" + clientIp;
        Object oldToken = redisUtil.get(ipIndexKey);
        if (oldToken != null && !String.valueOf(oldToken).isEmpty()
                && !String.valueOf(oldToken).equals(token)) {
            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + oldToken);
            redisUtil.del(AuthConst.PREFIX_ONLINE_META + oldToken);
        }
        redisUtil.set(AuthConst.PREFIX_USER_TOKEN + token, token, ttlSeconds);
        Map<String, Object> tokenMeta = new HashMap<>();
        tokenMeta.put("userName", userName);
        tokenMeta.put("token", token);
        tokenMeta.put("ip", clientIp);
        tokenMeta.put("deviceType", deviceType);
        // 会话ID：与会话/终端绑定，作为"记住我"刷新令牌的绑定键，使强制退出能精确定位；
        // 能识别原会话时复用原会话ID（本机刷新令牌与长连接继续有效），否则生成新的
        tokenMeta.put("sessionId", StringUtil.isNotEmpty(resolvedSessionId)
                ? resolvedSessionId
                : UUID.randomUUID().toString().replace("-", ""));
        redisUtil.set(AuthConst.PREFIX_ONLINE_META + token, JsonUtils.toJson(tokenMeta), ttlSeconds);
        redisUtil.set(ipIndexKey, token, ttlSeconds);

        //5、返回与 /login 一致的用户信息结构
        Map<String, Object> data = new HashMap<>();
        data.put("id", userData.get("id"));
        data.put("userName", userName);
        data.put("nickName", userData.get("nickName"));
        data.put("mobile", userData.get("mobile"));
        data.put("email", userData.get("email"));
        data.put("avatar", userData.get("avatar"));
        data.put("token", token);
        //以数据库最新标志位为准（改密成功时已置 0）
        Object pwdResetRequiredObj = userData.get("pwdResetRequired");
        int pwdResetRequired = 0;
        if (pwdResetRequiredObj instanceof Number) {
            pwdResetRequired = ((Number) pwdResetRequiredObj).intValue();
        }
        data.put("pwdResetRequired", Integer.valueOf(1).equals(pwdResetRequired) ? 1 : 0);
        return data;
    }
}
