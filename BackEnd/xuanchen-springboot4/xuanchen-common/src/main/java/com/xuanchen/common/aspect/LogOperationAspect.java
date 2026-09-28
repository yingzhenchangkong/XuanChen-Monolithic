package com.xuanchen.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.xuanchen.common.aspect.annotation.LogOperation;
import com.xuanchen.common.service.ILogOperationServiceCommon;
import com.xuanchen.common.utils.IPUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AOP切面-->操作日志记录
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogOperationAspect {
    private final ILogOperationServiceCommon logOperationServiceCommon;

    /**
     * 日志参数列最大长度，避免超大报文入库
     */
    private static final int MAX_PARAM_LENGTH = 4000;

    /**
     * 敏感字段（密码、令牌等）脱敏：键名包含这些词时值替换为 ***
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(\"[^\"]*(?:password|passwd|pwd|secret|token|credential)[^\"]*\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
            Pattern.CASE_INSENSITIVE);

    /**
     * 定义切点Pointcut
     */
    @Pointcut("@annotation(com.xuanchen.common.aspect.annotation.LogOperation)")
    public void annotationPointcut() {
    }

    /**
     * 环绕通知
     */
    @Around("annotationPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取注解信息
        LogOperation logOperation = method.getAnnotation(LogOperation.class);
        if (logOperation == null) {
            return joinPoint.proceed(); // 如果没有注解，直接执行方法
        }
        String operationType = logOperation.type(); // 操作类型
        String module = logOperation.module();       // 模块名称
        // 获取请求信息（假设通过 RequestContextHolder 获取）
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String requestUrl = request.getRequestURI();
        String requestParams = buildRequestParams(request, joinPoint.getArgs());
        String ipAddress = IPUtil.getClientIpAddress(request);
        // 用户名取自 Spring Security 上下文：该主体只在 JWT 过滤器验签通过后写入，
        // 不能直接解码请求头中的 token（旧写法不验签，可被伪造）
        String userName = getLoginUser();

        Object result;
        try {
            // 执行目标方法
            result = joinPoint.proceed();
        } catch (Exception e) {
            // 记录操作失败日志（status 必须为 false）
            safeRecord(userName, module, ipAddress, operationType, requestUrl, requestParams,
                    false, "操作失败:" + e.getMessage());
            throw e; // 抛出异常
        }
        // 记录操作成功日志
        safeRecord(userName, module, ipAddress, operationType, requestUrl, requestParams,
                true, "操作成功!");
        return result;
    }

    /**
     * 组装请求参数：query/form 参数 + 请求体（方法入参中的可序列化对象），敏感值脱敏后截断
     */
    private String buildRequestParams(HttpServletRequest request, Object[] args) {
        StringBuilder params = new StringBuilder();
        // 1、query/form 参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            params.append(entry.getKey()).append("=").append(String.join(",", entry.getValue())).append("&");
        }
        // 2、请求体：取方法入参中可 JSON 化的业务参数（排除 request/response/文件/流等容器对象）
        List<Object> bodyArgs = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                if (isLoggableArg(arg)) {
                    bodyArgs.add(arg);
                }
            }
        }
        if (!bodyArgs.isEmpty()) {
            if (!params.isEmpty()) {
                params.append(" ");
            }
            params.append("body=");
            try {
                String json = JSON.toJSONString(bodyArgs);
                params.append(maskSensitive(json));
            } catch (Exception e) {
                params.append("[unserializable]");
            }
        }
        if (params.length() > MAX_PARAM_LENGTH) {
            return params.substring(0, MAX_PARAM_LENGTH);
        }
        return params.toString();
    }

    /**
     * 判断入参是否适合写入日志：排除 Servlet 容器对象、文件上传、流等不可序列化/无意义对象
     */
    private boolean isLoggableArg(Object arg) {
        if (arg == null) {
            return false;
        }
        return !(arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof MultipartFile
                || arg instanceof MultipartHttpServletRequest
                || arg instanceof InputStream
                || arg instanceof OutputStream
                || arg instanceof Reader
                || arg instanceof Writer
                || arg instanceof byte[]
                || arg instanceof Byte[]);
    }

    /**
     * 敏感字段值脱敏
     */
    private String maskSensitive(String json) {
        return SENSITIVE_PATTERN.matcher(json).replaceAll("$1\"***\"");
    }

    /**
     * 获取已通过验签的登录用户名（未登录/匿名访问时返回空串）
     */
    private String getLoginUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                String name = authentication.getName();
                return name == null ? "" : name;
            }
        } catch (Exception e) {
            log.debug("获取登录用户失败", e);
        }
        return "";
    }

    /**
     * 记录操作日志本身失败时不能影响业务方法与原始异常
     */
    private void safeRecord(String userName, String module, String ipAddress, String operationType,
                            String requestUrl, String requestParams, boolean status, String description) {
        try {
            logOperationServiceCommon.recordOperationLog(userName, module, ipAddress, operationType, requestUrl,
                    requestParams, status, description);
        } catch (Exception e) {
            log.warn("操作日志记录失败 url={} type={}", requestUrl, operationType, e);
        }
    }
}
