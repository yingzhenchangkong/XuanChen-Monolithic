package com.xuanchen.auth.shiro.filter;

import com.xuanchen.auth.shiro.config.JwtToken;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.utils.StringUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 鉴权登录拦截器
 *
 * @author XuanChen
 * @date 2025-03-12
 */
public class JwtFilter extends BasicHttpAuthenticationFilter {
    /**
     * 执行登录认证
     * 如果带有token，则对token进行检查，否则直接通过
     *
     * @param request
     * @param response
     * @param mappedValue
     * @return
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        try {
            executeLogin(request, response);
            return true;
        } catch (Exception ex) {
            JwtUtil.responseError(response, 401, "Token失效，请重新登录!");
            return false;
        }
    }

    /**
     * 登录验证
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader(AuthConst.XC_ACCESS_TOKEN);
        JwtToken jwtToken = new JwtToken(token);
        Subject subject = getSubject(request, response);
        subject.login(jwtToken);
        return true;
    }

    /**
     * 对跨域提供支持
     * 这个是shiro的跨域，如果不写会导致携带token请求时报401错误
     * 已知：不写 OPTIONS 请求会 executeLogin 异常，GET、POST不会
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        //必需 设置允许客户端请求的域名 多个域名以逗号分隔 也可以设置成* 即允许所有源访问
        httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        //必需 设置允许客户端请求的方法 多个方法以逗号分隔
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
        //可选 允许客户端提交的Header
        String requestHeaders = httpServletRequest.getHeader("Access-Control-Request-Headers");
        if (StringUtil.isNotEmpty(requestHeaders)) {
            httpServletResponse.setHeader("Access-Control-Allow-Headers", requestHeaders);
        }
        //可选 允许客户端携带凭证信息(是否允许发送Cookie)
        httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
        // 跨域时会首先发送一个option请求，这里我们给option请求直接返回正常状态
        if (RequestMethod.OPTIONS.name().equalsIgnoreCase(httpServletRequest.getMethod())) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            return false;
        }
        return super.preHandle(request, response);
    }
}
