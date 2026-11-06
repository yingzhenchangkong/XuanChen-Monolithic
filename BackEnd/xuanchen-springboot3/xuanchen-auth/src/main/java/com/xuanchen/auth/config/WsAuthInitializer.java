package com.xuanchen.auth.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.server.WsAuthBridge;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * WebSocket 握手认证注册器
 * <p>
 * 启动时把完整的令牌认证实现注册给 common 模块的 {@link WsAuthBridge}。
 * 校验口径必须与 {@link com.xuanchen.auth.shiro.realm.ShiroRealm} 完全一致：
 * Redis 会话存在 + JWT 签名/用户名合法 + 账号未删除（delFlag）/未冻结（status）。
 * 因此管理员冻结或删除用户后，其旧令牌不仅不能用于 HTTP，也无法再建立 WS 连接。
 *
 * @author XuanChen
 * @date 2026-09-29
 */
@Component
@Slf4j
public class WsAuthInitializer {

    @Autowired
    private IAuthService authService;

    @Autowired
    private RedisUtil redisUtil;

    @PostConstruct
    public void init() {
        WsAuthBridge.registerTokenVerifier(this::verify);
        log.info("WebSocket 握手认证器已注册（Redis 会话 + JWT 签名 + 账号状态）");
    }

    /**
     * @return 校验通过返回会话上下文（用户名 + 会话ID），任一环节失败返回 null
     */
    private WsAuthBridge.SessionContext verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String username = JwtUtil.getUsername(token);
        if (username == null) {
            return null;
        }
        LambdaQueryWrapper<Auth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Auth::getUserName, username);
        Auth authUser = authService.getOne(queryWrapper);
        if (authUser == null) {
            return null;
        }
        if (authUser.getDelFlag() != null && authUser.getDelFlag() != 0) {
            return null;
        }
        if (authUser.getStatus() == null || authUser.getStatus() != 1) {
            return null;
        }
        Object cachedToken = redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
        if (cachedToken == null) {
            return null;
        }
        if (!JwtUtil.verify(String.valueOf(cachedToken), username)) {
            return null;
        }
        // 会话ID与 HTTP 侧同一标识，供"登出/被踢"按会话精确断开 WebSocket；
        // 历史会话元数据无该字段时为 null，调用方退化为按令牌匹配
        return new WsAuthBridge.SessionContext(username, extractSessionId(token));
    }

    /**
     * 从在线会话元数据（PREFIX_ONLINE_META_）中读取会话ID；缺失/损坏返回 null
     */
    private String extractSessionId(String token) {
        Object metaObj = redisUtil.get(AuthConst.PREFIX_ONLINE_META + token);
        if (!(metaObj instanceof String meta) || !meta.startsWith("{")) {
            return null;
        }
        try {
            Object sessionId = JsonUtils.parseToMap(meta).get("sessionId");
            return sessionId == null ? null : sessionId.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
