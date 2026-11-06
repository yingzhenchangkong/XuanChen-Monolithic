package com.xuanchen.auth.shiro.exception;

import com.xuanchen.common.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.AuthorizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Shiro 鉴权异常处理
 * 方法级 @RequiresRoles/@RequiresPermissions 校验不通过时 Shiro 抛出
 * AuthorizationException，这里统一转换为 403 JSON，与前端 httpRequest 的 403 分支契约一致。
 * 业务异常与未捕获系统异常由 xuanchen-common 的 GlobalExceptionHandler 统一处理，
 * 本类只按更具体的鉴权异常类型优先接管。
 *
 * @author XuanChen
 * @date 2026-09-29
 */
@RestControllerAdvice
@Slf4j
public class ShiroAuthzExceptionHandler {

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Result<?>> handleAuthorizationException(AuthorizationException e, HttpServletRequest request) {
        log.warn("访问被拒绝 uri={} method={} reason={}", request.getRequestURI(), request.getMethod(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.forbidden("权限不足，禁止访问！"));
    }
}
