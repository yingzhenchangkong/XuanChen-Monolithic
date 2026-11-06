package com.xuanchen.common.config;

import com.xuanchen.common.server.WsAuthBridge;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebSocket 握手安全配置器
 * <p>
 * 1、Origin 白名单校验，防止跨站 WebSocket 劫持（CSWSH）；
 * 2、握手时校验查询参数 token（与 HTTP 接口同一套 Redis 会话 + JWT 签名 + 账号状态校验，
 * 具体实现由 auth 侧经 {@link WsAuthBridge} 注册），通过后把用户名放入握手用户属性，
 * {@code @OnOpen} 中无用户名立即拒绝；
 * 3、WebSocket 容器会自行 new 配置器实例（非 Spring 管理），故通过静态字段桥接 Bean。
 *
 * @author XuanChen
 * @date 2026-09-17
 */
@Slf4j
@Component
public class WebSocketAuthConfigurator extends ServerEndpointConfig.Configurator {

    private static volatile Set<String> allowedOrigins = Set.of();

    public WebSocketAuthConfigurator() {
        // WebSocket 容器（Tomcat）注册端点时通过无参构造自行实例化；
        // 该实例的依赖通过 Spring 单例构造时写入的静态字段桥接获取
    }

    @Autowired
    public WebSocketAuthConfigurator(@Value("${xuanchen.cors.allowed-origins:}") String origins) {
        WebSocketAuthConfigurator.allowedOrigins = origins == null ? Set.of()
                : Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Origin 校验：浏览器 WebSocket 握手必带 Origin，不在白名单一律拒绝；
     * 无 Origin（非浏览器客户端）同样拒绝
     */
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        boolean allowed = originHeaderValue != null && allowedOrigins.contains(originHeaderValue);
        if (!allowed) {
            log.warn("【websocket】拒绝非白名单来源的握手: {}", originHeaderValue);
        }
        return allowed;
    }

    /**
     * 握手阶段校验 token，通过则记录用户名、访问令牌与会话ID。
     * <p>
     * 令牌与会话ID写入握手用户属性，供 {@link com.xuanchen.common.server.WebSocketServer}
     * 在"登出/被踢"时精确断开对应的长连接。
     */
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        super.modifyHandshake(sec, request, response);
        String token = getToken(request);
        if (token == null) {
            return;
        }
        WsAuthBridge.SessionContext context = authenticate(token);
        if (context != null) {
            sec.getUserProperties().put("userName", context.userName());
            sec.getUserProperties().put("token", token);
            if (context.sessionId() != null) {
                sec.getUserProperties().put("sessionId", context.sessionId());
            }
        }
    }

    /**
     * 从握手查询参数中提取访问令牌；缺失返回 null
     */
    private String getToken(HandshakeRequest request) {
        Map<String, List<String>> params = request.getParameterMap();
        if (params == null) {
            return null;
        }
        List<String> tokens = params.get("token");
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        String token = tokens.get(0);
        return (token == null || token.isBlank()) ? null : token;
    }

    private WsAuthBridge.SessionContext authenticate(String token) {
        try {
            // 与 HTTP 侧同一套校验：JWT 解析 + Redis 会话存在 + JWT 签名/过期 + 账号未删除/未冻结
            WsAuthBridge.SessionContext context = WsAuthBridge.verify(token);
            if (context == null || context.userName() == null || context.userName().isBlank()) {
                log.warn("【websocket】拒绝令牌校验失败的握手");
                return null;
            }
            return context;
        } catch (Exception e) {
            log.warn("【websocket】握手 token 校验异常", e);
            return null;
        }
    }
}
