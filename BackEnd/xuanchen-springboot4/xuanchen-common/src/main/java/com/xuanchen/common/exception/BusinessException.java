package com.xuanchen.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 *
 * @author XuanChen
 * @date 2026-05-28
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
