package com.xuanchen.common.exception;

import com.xuanchen.common.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 *
 * @author XuanChen
 * @date 2026-05-28
 */
@Slf4j
@RestControllerAdvice //用于处理 @RestController 注解的类中的异常
public class GlobalExceptionHandler {
    /**
     * 处理权限不足异常（@PreAuthorize 方法鉴权拒绝时抛出）
     *
     * @param e 访问拒绝异常
     * @return 403 错误信息
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问被拒绝 uri={} method={} reason={}", request.getRequestURI(), request.getMethod(), e.getMessage());
        return Result.forbidden("权限不足，禁止访问！");
    }

    /**
     * 处理自定义业务异常
     *
     * @param e 业务异常
     * @return 错误信息
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        // 业务异常属于预期内的控制流，仅记录一行便于追溯，不打印堆栈
        log.warn("业务异常 uri={} code={} msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return new Result<>(e.getCode(), e.getMessage(), null);
    }

    /**
     * 处理 404 异常。
     * 生效前提（已在 application-common.yml 配置）：
     * spring.mvc.throw-exception-if-no-handler-found=true
     * spring.web.resources.add-mappings=false
     *
     * @param e 404 找不到接口异常
     * @return 错误信息
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("接口不存在 uri={} method={}", request.getRequestURI(), request.getMethod());
        return new Result<>(HttpStatus.NOT_FOUND.value(), "请求的接口不存在", null);
    }

    /**
     * 兜底处理所有未捕获的系统异常。
     * 必须记录完整堆栈，线上排障依赖此日志；对外只返回脱敏的通用提示，避免泄露内部实现细节。
     *
     * @param e 系统异常
     * @return 错误信息
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("未捕获的系统异常 uri={} method={}", request.getRequestURI(), request.getMethod(), e);
        return new Result<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误，请稍后重试", null);
    }
}
