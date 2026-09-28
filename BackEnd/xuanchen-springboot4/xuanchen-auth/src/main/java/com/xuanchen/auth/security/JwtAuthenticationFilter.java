package com.xuanchen.auth.security;

import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * JWT 认证过滤器
 *
 * @author XuanChen
 * @date 2026-03-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;
    private final RedisUtil redisUtil;

    /**
     * 处于"必须改密"状态的用户额外允许访问的精确路径：
     * 本人修改密码、登出。其余业务请求一律拒绝，防止拿到临时密码后不改密直接使用系统。
     * 注意：SecurityConfig 中 permitAll 的公开接口（/login、/captcha/**、
     * /system/config/getConfigKeyValue、/filemanage/static/**、/ws）不在强制改密拦截范围内，
     * 否则用户在改密引导页连验证码/公开配置都加载不了，形成"想改密也改不了"的死锁。
     */
    private static final Set<String> PWD_RESET_ALLOWED_PATHS = Set.of(
            "/system/user/changePassword",
            "/logout",
            "/login",
            "/system/config/getConfigKeyValue",
            "/ws"
    );

    /**
     * 强制改密状态下同样放行的路径前缀（对应 SecurityConfig 的 permitAll 通配规则）
     */
    private static final Set<String> PWD_RESET_ALLOWED_PREFIXES = Set.of(
            "/captcha/",
            "/filemanage/static/"
    );

    /**
     * 强制改密专用业务码（HTTP 状态仍为 403）：前端据此把用户引导到独立改密页，
     * 与普通"权限不足"的 403 区分开
     */
    public static final int PWD_RESET_REQUIRED_CODE = 4030;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(AuthConst.XC_ACCESS_TOKEN);

        if (!StringUtil.isEmpty(token)) {
            // 整个认证过程异常隔离：畸形/篡改 token、Redis 故障等任何异常都不得抛出 500，
            // 一律 fail-closed（不建立认证上下文，按匿名继续，最终由授权层返回 401）
            try {
                String username = JwtUtil.getUsername(token);

                if (!StringUtil.isEmpty(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 验证token是否在redis中
                    String redisToken = (String) redisUtil.get(AuthConst.PREFIX_USER_TOKEN + token);
                    if (redisToken != null) {
                        // Redis 会话存在后再校验 JWT 签名/过期/用户名：Redis 是会话存在性依据，
                        // 签名校验保证 token 确由本服务端签发且未过期（纵深防御，密钥见 xuanchen.jwt.secret）
                        if (!JwtUtil.verify(token, username)) {
                            // 签名非法或已过期：清理可能残留的 Redis 会话，按未认证处理（401）
                            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
                            filterChain.doFilter(request, response);
                            return;
                        }
                        UserDetails userDetails;
                        try {
                            userDetails = userDetailsService.loadUserByUsername(username);
                        } catch (UsernameNotFoundException e) {
                            // 用户已被删除：清除残留会话，按未认证处理（401）
                            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
                            filterChain.doFilter(request, response);
                            return;
                        }
                        // 账号已被冻结：立即清除会话且不建立认证上下文，交由 401 入口点处理
                        if (!userDetails.isEnabled()) {
                            redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
                            filterChain.doFilter(request, response);
                            return;
                        }
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 强制改密：临时密码/被重置密码的用户只允许改密/登出及公开接口，其余请求在此硬拦截
                        if (userDetails instanceof LoginUser loginUser && loginUser.isPwdResetRequired()
                                && !isPwdResetAllowedPath(request.getRequestURI())) {
                            writePwdResetRequiredResponse(response);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                // 畸形 token 解析失败、Redis 连接异常等：确保不留半成品认证上下文
                SecurityContextHolder.clearContext();
                log.warn("JWT 认证处理异常，已按未认证请求放行至授权层: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 是否为强制改密状态下放行的路径：精确白名单 + 公开路径前缀
     */
    private boolean isPwdResetAllowedPath(String uri) {
        if (uri == null) {
            return false;
        }
        if (PWD_RESET_ALLOWED_PATHS.contains(uri)) {
            return true;
        }
        for (String prefix : PWD_RESET_ALLOWED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 强制改密拦截响应：HTTP 403 + 专用业务码 4030 + 统一 JSON 结构
     */
    private void writePwdResetRequiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<String> result = new Result<>(PWD_RESET_REQUIRED_CODE,
                "当前为初始/重置密码，请先修改密码后再进行其他操作！", null);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
