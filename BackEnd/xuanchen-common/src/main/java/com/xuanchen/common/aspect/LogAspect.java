package com.xuanchen.common.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 日志AOP类
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Aspect
@Component
public class LogAspect {
    // 定义切入点：拦截所有 controller 包下的方法
    @Pointcut("@annotation(com.xuanchen.common.aspect.annotation.LogOperation)")
    public void controllerMethods() {
    }

    // 前置通知：方法执行前记录日志
    @Before("controllerMethods()")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("执行方法：" + joinPoint.getSignature().getName());
    }

    // 环绕通知：控制方法执行并记录耗时
    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed(); // 执行目标方法
            long endTime = System.currentTimeMillis();
            System.out.println("方法耗时：" + (endTime - startTime) + "ms");
            return result;
        } catch (Exception e) {
            System.out.println("方法执行异常：" + e.getMessage());
            throw e;
        }
    }
}
