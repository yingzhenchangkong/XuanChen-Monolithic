package com.xuanchen.auth.shiro.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 自定义 Tomcat 过滤器
 *
 * @author XuanChen
 * @date 2025-03-12
 */
@Component
public class CORSFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        //必需 设置允许客户端请求的域名 多个域名以逗号分隔 也可以设置成* 即允许所有源访问
        httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        //可选 允许客户端携带凭证信息(是否允许发送Cookie)
        httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
        //必需 设置允许客户端请求的方法 多个方法以逗号分隔
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
        // 最大时间
        httpServletResponse.setHeader("Access-Control-Max-Age", "3600");
        //可选 允许客户端提交的Header  需要放行header头部字段 如需鉴权字段，自行添加，如Authorization
        //httpServletResponse.setHeader("Access-Control-Allow-Headers", "content-type,x-requested-with,token,Authorization,authorization");
        String requestHeaders = httpServletRequest.getHeader("Access-Control-Request-Headers");
        if (StringUtils.hasLength(requestHeaders)) {
            httpServletResponse.setHeader("Access-Control-Allow-Headers", requestHeaders);
        }
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            //TODO 异常时记录到日志
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
