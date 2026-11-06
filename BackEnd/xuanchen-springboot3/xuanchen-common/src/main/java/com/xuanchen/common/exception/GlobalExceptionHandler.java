package com.xuanchen.common.exception;

import com.xuanchen.common.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器。
 * <p>
 * Shiro 鉴权类异常（AuthorizationException 等）由 xuanchen-auth 的 ShiroAuthzExceptionHandler
 * 按更具体的异常类型优先处理，本类只负责业务异常与未捕获系统异常的统一 JSON 响应。
 *
 * @author XuanChen
 * @date 2026-05-28
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
     * 上传文件超过 multipart 大小上限（容器层在进入 Controller 前抛出）
     *
     * @param e 超限异常
     * @return 413 错误信息
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<?> handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("上传文件超限 uri={} msg={}", request.getRequestURI(), e.getMessage());
        return new Result<>(HttpStatus.PAYLOAD_TOO_LARGE.value(), "上传文件大小超出限制！", null);
    }

    /**
     * 处理 404：请求未命中任何接口。
     * 生效前提（已在 application-common.yml 配置）：
     * spring.mvc.throw-exception-if-no-handler-found=true
     * spring.web.resources.add-mappings=false
     * 注意：需携带有效 token 通过 Shiro 过滤器后才会到达此处；匿名白名单之外的不存在路径
     * 仍由 JwtFilter 先返回 401。返回统一 Result JSON，避免浏览器/文件下载场景拿到 HTML 错误页。
     *
     * @param e 404 找不到接口异常
     * @return 错误信息
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("接口不存在 uri={} method={}", request.getRequestURI(), request.getMethod());
        return Result.notFound("请求的接口不存在");
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
        return Result.error("服务器内部错误，请稍后重试");
    }
}
