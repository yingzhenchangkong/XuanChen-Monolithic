package com.xuanchen.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CORS 允许来源白名单持有者。
 * <p>
 * 与 WebSocket 握手 Origin 白名单共用同一配置项 {@code xuanchen.cors.allowed-origins}
 * （逗号分隔的精确 Origin，环境变量 {@code XUANCHEN_CORS_ALLOWED_ORIGINS}）。
 * 作为普通 Spring 单例使用：外层 {@code CORSFilter} 由容器扫描注入；
 * Shiro 的 {@code JwtFilter} 虽由 {@code ShiroConfig} 中 {@code new JwtFilter(corsProperties)}
 * 手工实例化，但白名单由 {@code ShiroConfig} 方法参数注入后经构造器传入，无需静态桥接。
 * <p>
 * 注意：WebSocket 的 {@code WebSocketAuthConfigurator} 由 Tomcat 按无参构造自行实例化，
 * 无法走此路径，仍在 xuanchen-common 内保留其独立的静态桥。
 * <p>
 * 安全约束：{@code Access-Control-Allow-Credentials=true} 时禁止回显任意 Origin，
 * 否则等价于允许任意网站携带凭证（含 XC-ACCESS-TOKEN 所在的跨域响应被读取）发起跨域请求；
 * 未配置任何来源（空白名单）时一律不返回 CORS 头（fail-closed，同源/curl 不受影响）。
 *
 * @author XuanChen
 * @date 2026-09-29
 */
@Component
public class CorsProperties {

    private final Set<String> allowedOrigins;

    public CorsProperties(@Value("${xuanchen.cors.allowed-origins:}") String origins) {
        this.allowedOrigins = origins == null ? Set.of()
                : Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Origin 是否在白名单内：精确匹配，区分大小写；null/空白一律不允许
     * （同源请求浏览器不依赖 CORS 头，非浏览器客户端不受影响）
     */
    public boolean isAllowed(String origin) {
        return origin != null && !origin.isBlank() && allowedOrigins.contains(origin);
    }
}
