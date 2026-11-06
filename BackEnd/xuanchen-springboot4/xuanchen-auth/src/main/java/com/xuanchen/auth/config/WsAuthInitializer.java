package com.xuanchen.auth.config;

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
 * 校验口径必须与 HTTP 侧（{@code JwtAuthenticationFilter} + {@code UserDetailsServiceImpl}）
 * 完全一致：JWT 可解析 + 账号存在且未删除（delFlag）/未冻结（status）
 * + Redis 会话存在 + JWT 签名/过期合法。
 * 因此管理员冻结或删除用户后，其旧令牌不仅不能用于 HTTP，也无法再建立 WS 连接，
 * 不依赖"改状态时是否记得踢会话"这类外围约定。
 *
 * @author XuanChen
 * @date 2026-10-08
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
        if (username == null || username.isBlank()) {
            return null;
        }
        // 复用 IAuthService 统一入口；@TableLogic 已自动追加 del_flag = 0，
        // 下面仍保留显式 delFlag 校验作为纵深防御
        Auth authUser = authService.getByUserName(username);
        if (authUser == null) {
            return null;
        }
        if (authUser.getDelFlag() != null && authUser.getDelFlag() != 0) {
            return null;
        }
        if (authUser.getStatus() == null || authUser.getStatus() != 1) {
            return null;
        }
        // Redis 会话存在性：登出、改密踢出、管理员强制退出都会删除该键
        Object cached = redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
        if (cached == null) {
            return null;
        }
        // Redis 会话存在后再校验 JWT 签名/过期/用户名（与 HTTP 过滤器同一实现）
        if (!JwtUtil.verify(token, username)) {
            return null;
        }
        // 会话ID与 HTTP 侧同一标识，供"登出/被踢"按会话精确断开 WebSocket；
        // 历史会话元数据无该字段时为 null，调用方退化为按令牌匹配
        return new WsAuthBridge.SessionContext(username, extractSessionId(cached));
    }

    /**
     * 从会话元数据 JSON 中读取会话ID；缺失/损坏返回 null
     */
    private String extractSessionId(Object cachedMeta) {
        if (!(cachedMeta instanceof String meta) || !meta.startsWith("{")) {
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
