package com.xuanchen.common.aspect;

import com.xuanchen.common.aspect.annotation.LogOperation;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.service.ILogOperationServiceCommon;
import com.xuanchen.common.utils.IPUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * AOP切面-->操作日志记录
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Aspect
@Component
public class LogOperationAspect {
    @Autowired
    private ILogOperationServiceCommon logOperationServiceCommon;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

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
        String requestParams = getRequestParams(request);
        String ipAddress = IPUtil.getClientIpAddress(request);
        // 获取当前用户信息（假设通过某种方式获取用户名）
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")); // 需要根据实际业务逻辑实现
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            // 记录操作成功日志
            Boolean status = true;
            String description = "操作成功!";
            logOperationServiceCommon.recordOperationLog(userName, module, ipAddress, operationType, requestUrl,
                    requestParams, status, description);
            return result;
        } catch (Exception e) {
            // 记录操作失败日志
            Boolean status = true;
            String description = "操作失败:" + e.getMessage();
            logOperationServiceCommon.recordOperationLog(userName, module, ipAddress, operationType, requestUrl,
                    requestParams, status, description);
            throw e; // 抛出异常
        }
    }

    private String getRequestParams(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        StringBuilder params = new StringBuilder();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            params.append(entry.getKey()).append("=").append(String.join(",", entry.getValue())).append("&");
        }
        return params.toString();
    }
}
