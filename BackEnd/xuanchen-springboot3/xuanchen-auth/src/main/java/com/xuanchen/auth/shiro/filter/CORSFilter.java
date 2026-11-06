package com.xuanchen.auth.shiro.filter;

import com.xuanchen.auth.config.CorsProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 自定义 Tomcat 过滤器：CORS 跨域响应头统一处理（最外层，先于 Shiro 链执行）。
 * <p>
 * 安全要求：仅当请求 Origin 精确命中 {@code xuanchen.cors.allowed-origins} 白名单时
 * 才回显 Allow-Origin 并允许凭证；严禁无条件反射 Origin + Allow-Credentials=true
 * （该组合等价于允许任意网站携带凭证发起跨域请求并读取响应）。
 * 非白名单来源的预检（OPTIONS + Access-Control-Request-Method）直接 403 且不带任何
 * CORS 头；非白名单来源的实际请求继续放行由业务处理，但响应不带 CORS 头，浏览器
 * 会阻止跨域脚本读取（同源请求与 curl 等非浏览器客户端不受影响）。
 *
 * @author XuanChen
 * @date 2025-03-12
 */
@Component
public class CORSFilter implements Filter {

    private static final String ALLOWED_METHODS = "GET,POST,OPTIONS,PUT,DELETE";

    private final CorsProperties corsProperties;

    public CORSFilter(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        String origin = httpServletRequest.getHeader("Origin");
        // 响应随 Origin 条件变化，恒加 Vary 防止缓存把某来源的 CORS 响应污染给其他来源
        httpServletResponse.setHeader("Vary", "Origin");

        if (corsProperties.isAllowed(origin)) {
            //精确回显命中白名单的来源（credentials=true 时浏览器禁止使用 "*"）
            httpServletResponse.setHeader("Access-Control-Allow-Origin", origin);
            //允许携带凭证信息（Cookie 及未来可能的凭证场景）
            httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpServletResponse.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
            httpServletResponse.setHeader("Access-Control-Max-Age", "3600");
            //回显预检中声明的自定义请求头（来源已受信任，等价于 Spring allowedHeaders("*")）
            String requestHeaders = httpServletRequest.getHeader("Access-Control-Request-Headers");
            if (StringUtils.hasLength(requestHeaders)) {
                httpServletResponse.setHeader("Access-Control-Allow-Headers", requestHeaders);
            }
        } else if (isPreFlight(httpServletRequest)) {
            //非白名单来源的预检：无任何 CORS 头并直接拒绝，浏览器判定跨域失败
            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    /**
     * CORS 预检请求判定：OPTIONS 方法且带 Access-Control-Request-Method
     */
    private boolean isPreFlight(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                && StringUtils.hasLength(request.getHeader("Access-Control-Request-Method"));
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
