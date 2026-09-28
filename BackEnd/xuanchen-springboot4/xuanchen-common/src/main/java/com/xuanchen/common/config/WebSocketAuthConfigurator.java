package com.xuanchen.common.config;

import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.utils.RedisUtil;
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
 * 2、握手时校验查询参数 token 对应的 Redis 会话是否存在（与 HTTP 接口同一套会话），
 * 通过后把用户名放入握手用户属性，{@code @OnOpen} 中无用户名立即拒绝；
 * 3、WebSocket 容器会自行 new 配置器实例（非 Spring 管理），故通过静态字段桥接 Bean。
 *
 * @author XuanChen
 * @date 2026-09-17
 */
@Slf4j
@Component
public class WebSocketAuthConfigurator extends ServerEndpointConfig.Configurator {

    private static volatile RedisUtil redisUtil;
    private static volatile Set<String> allowedOrigins = Set.of();

    public WebSocketAuthConfigurator() {
        // WebSocket 容器（Tomcat）注册端点时通过无参构造自行实例化；
        // 该实例的依赖通过 Spring 单例构造时写入的静态字段桥接获取
    }

    @Autowired
    public WebSocketAuthConfigurator(RedisUtil redisUtil,
                                     @Value("${xuanchen.cors.allowed-origins:}") String origins) {
        WebSocketAuthConfigurator.redisUtil = redisUtil;
        WebSocketAuthConfigurator.allowedOrigins = origins == null ? Set.of()
                : Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Origin 校验：浏览器 WebSocket 握手必带 Origin，不在白名单一律拒绝
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
     * 握手阶段校验 token，通过则记录用户名
     */
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        super.modifyHandshake(sec, request, response);
        String userName = authenticate(request);
        if (userName != null) {
            sec.getUserProperties().put("userName", userName);
        }
    }

    private String authenticate(HandshakeRequest request) {
        Map<String, List<String>> params = request.getParameterMap();
        if (params == null) {
            return null;
        }
        List<String> tokens = params.get("token");
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        String token = tokens.get(0);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Object metaObj = redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
            //会话元数据为 JSON 串，旧版可能直接存 token 字符串；仅认可含用户名的会话
            if (!(metaObj instanceof String meta) || !meta.startsWith("{")) {
                return null;
            }
            int idx = meta.indexOf("\"username\"");
            if (idx < 0) {
                return null;
            }
            int colon = meta.indexOf(':', idx);
            int start = meta.indexOf('"', colon + 1) + 1;
            int end = meta.indexOf('"', start);
            if (start <= 0 || end <= start) {
                return null;
            }
            String userName = meta.substring(start, end);
            if (userName.isBlank()) {
                return null;
            }
            // 会话存在后再校验 JWT 签名/过期/用户名（与 HTTP 过滤器同一套实现，经 WsAuthBridge 桥接）
            if (!com.xuanchen.common.server.WsAuthBridge.verify(token, userName)) {
                log.warn("【websocket】拒绝签名校验失败的握手, user={}", userName);
                return null;
            }
            return userName;
        } catch (Exception e) {
            log.warn("【websocket】握手 token 校验异常", e);
            return null;
        }
    }
}
