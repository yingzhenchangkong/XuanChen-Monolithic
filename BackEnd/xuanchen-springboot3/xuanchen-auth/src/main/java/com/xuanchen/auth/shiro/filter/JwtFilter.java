package com.xuanchen.auth.shiro.filter;

import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.config.CorsProperties;
import com.xuanchen.auth.shiro.config.JwtToken;
import com.xuanchen.auth.shiro.exception.JwtAuthException;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 鉴权登录拦截器
 *
 * @author XuanChen
 * @date 2025-03-12
 */
@Slf4j
public class JwtFilter extends BasicHttpAuthenticationFilter {

    /**
     * 强制改密专用业务码（HTTP 状态仍为 403）：前端据此把用户引导到独立改密页，
     * 与普通"权限不足"的 403 区分开。需与前端 PWD_RESET_REQUIRED_CODE 保持一致
     */
    public static final int PWD_RESET_REQUIRED_CODE = 4030;

    /**
     * CORS 白名单持有者。本过滤器由 ShiroConfig 中 {@code new JwtFilter(corsProperties)}
     * 手工实例化、不经 Spring 容器，无法使用注入，故由 ShiroConfig 方法参数取得
     * Spring 单例后经构造器传入。
     */
    private final CorsProperties corsProperties;

    public JwtFilter(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * 处于"必须改密"状态的用户额外允许访问的精确路径：本人修改密码、登出。
     * ShiroConfig 中 anon 放行的公开接口（/login、/captcha/**、
     * /system/config/getConfigKeyValue、/filemanage/static/**、/ws）本就不走本过滤器，
     * 在此一并列出仅为白名单语义完整，防止调整过滤器链后形成"想改密也改不了"的死锁
     */
    private static final Set<String> PWD_RESET_ALLOWED_PATHS = Set.of(
            "/system/account/changePassword",
            "/logout",
            "/login",
            "/system/config/getConfigKeyValue",
            "/ws"
    );

    /**
     * 强制改密状态下同样放行的路径前缀（对应 ShiroConfig 的 anon 通配规则）
     */
    private static final Set<String> PWD_RESET_ALLOWED_PREFIXES = Set.of(
            "/captcha/",
            "/filemanage/static/",
            "/ws/"
    );

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
            //登录态有效后再校验"必须改密"状态：拿到临时密码不改密不得使用任何业务功能
            Auth principal = (Auth) getSubject(request, response).getPrincipal();
            if (principal != null && Integer.valueOf(1).equals(principal.getPwdResetRequired())
                    && !isPwdResetAllowedPath((HttpServletRequest) request)) {
                writePwdResetRequired(response);
                return false;
            }
            return true;
        } catch (JwtAuthException ex) {
            //结构化认证失败：按具体原因回不同提示，服务端同时记录原因码便于排障
            String uri = ((HttpServletRequest) request).getRequestURI();
            //签名无效可能意味着有人伪造/篡改 token，用 warn 留痕；其余属正常鉴权失败用 debug
            if (ex.getReason() == JwtAuthException.Reason.SIGNATURE_INVALID) {
                log.warn("鉴权失败[{}] uri={}", ex.getReason(), uri);
            } else if (log.isDebugEnabled()) {
                log.debug("鉴权失败[{}] uri={}", ex.getReason(), uri);
            }
            JwtUtil.responseError(response, 401, ex.getReason().getClientMessage());
            return false;
        } catch (AuthenticationException ex) {
            //非 JwtAuthException 的 Shiro 认证异常：记录具体类型与消息，不再被"Token失效"一刀切掩盖
            log.warn("鉴权失败 type={} msg={}", ex.getClass().getSimpleName(), ex.getMessage());
            JwtUtil.responseError(response, 401, "Token失效，请重新登录!");
            return false;
        } catch (Exception ex) {
            //非认证类异常属服务端故障，error 记录完整堆栈，对客户端仍回 401 兜底
            log.error("鉴权过滤器异常", ex);
            JwtUtil.responseError(response, 401, "Token失效，请重新登录!");
            return false;
        }
    }

    /**
     * 请求路径是否属于强制改密状态下放行的白名单（精确匹配或前缀匹配）
     */
    private boolean isPwdResetAllowedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isEmpty()) {
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
     * 输出强制改密响应：HTTP 403 + 业务码 4030（不能复用 JwtUtil.responseError——
     * 它会把业务码直接写进 HTTP status，4030 不是合法状态码）
     */
    private void writePwdResetRequired(ServletResponse response) {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setStatus(HttpStatus.FORBIDDEN.value());
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        Result<String> jsonResult = new Result<>(PWD_RESET_REQUIRED_CODE,
                "当前为初始/重置密码，请先修改密码后再进行其他操作！");
        try (OutputStream os = httpServletResponse.getOutputStream()) {
            os.write(JsonUtils.toJson(jsonResult).getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (Exception ignored) {
            //响应写入失败（客户端已断开等）无需处理
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
     * <p>
     * 安全：外层 CORSFilter 已做白名单处理，这里作为 Shiro 链内的第二道防线，
     * 同样只回显命中 {@code xuanchen.cors.allowed-origins} 白名单的 Origin，
     * 严禁无条件反射 Origin + Allow-Credentials=true（否则任意网站可携带凭证跨域读取响应）。
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
        String origin = httpServletRequest.getHeader("Origin");
        //响应随 Origin 条件变化，恒加 Vary 防止缓存污染
        httpServletResponse.setHeader("Vary", "Origin");
        boolean originAllowed = corsProperties.isAllowed(origin);
        if (originAllowed) {
            //精确回显白名单来源（credentials=true 时浏览器禁止使用 "*"）
            httpServletResponse.setHeader("Access-Control-Allow-Origin", origin);
            //设置允许客户端请求的方法
            httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
            //回显预检中声明的自定义请求头
            String requestHeaders = httpServletRequest.getHeader("Access-Control-Request-Headers");
            if (StringUtil.isNotEmpty(requestHeaders)) {
                httpServletResponse.setHeader("Access-Control-Allow-Headers", requestHeaders);
            }
            //允许客户端携带凭证信息(是否允许发送Cookie)
            httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
        }
        // 跨域时会首先发送一个option请求：白名单来源直接返回正常状态，
        // 非白名单来源拒绝（外层 CORSFilter 通常已先于 Shiro 拦截，此处兜底）
        if (RequestMethod.OPTIONS.name().equalsIgnoreCase(httpServletRequest.getMethod())) {
            httpServletResponse.setStatus(originAllowed
                    ? HttpStatus.OK.value() : HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return super.preHandle(request, response);
    }
}
