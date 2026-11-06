package com.xuanchen.common.entity;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实体类-->通用返回结果
 * HTTP状态码	语义	        使用场景
 * 200	        成功	        操作成功、查询成功
 * 400	        请求错误	    参数校验失败、请求格式错误
 * 401	        未授权	    未登录、token过期、认证失败
 * 403	        禁止访问	    权限不足
 * 404	        未找到	    资源不存在
 * 409	        冲突	        数据重复、状态冲突
 * 500	        服务器错误	系统异常、不可预期错误
 *
 * @author XuanChen
 * @date 2025-03-05
 */
@Data
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 返回状态码
     */
    private int code;
    /**
     * 返回内容
     */
    private String msg;
    /**
     * 返回数据对象
     */
    private T data;

    /**
     * 构造函数 (无参)
     */
    public Result() {
    }

    /**
     * 构造函数 (状态码，返回内容)
     *
     * @param code 状态码
     * @param msg  返回内容
     */
    public Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 构造函数 (状态码，返回内容，数据对象)
     *
     * @param code 状态码
     * @param msg  返回内容
     * @param data 数据对象
     */
    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 返回成功消息
     *
     * @param msg  返回内容
     * @param data 数据对象
     * @return code-->200，msg-->返回内容，data-->数据对象
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(HttpStatus.OK.value(), msg, data);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回内容
     * @return code-->200，msg-->返回内容，data-->null
     */
    public static <T> Result<T> success(String msg) {
        return Result.success(msg, null);
    }

    /**
     * 返回成功消息
     *
     * @param data 返回数据
     * @return code-->200，msg-->操作成功，data-->data
     */
    public static <T> Result<T> success(T data) {
        return Result.success("操作成功", data);
    }

    /**
     * 返回成功消息
     *
     * @return code-->200，msg-->操作成功，data-->null
     */
    public static <T> Result<T> success() {
        return Result.success("操作成功");
    }

    /**
     * 请求错误
     *
     * @param msg 错误信息
     * @return code-->400，msg-->错误信息，data-->null
     */
    public static <T> Result<T> badRequest(String msg) {
        return new Result<>(HttpStatus.BAD_REQUEST.value(), msg, null);
    }

    /**
     * 未授权
     *
     * @param msg 错误信息
     * @return code-->401，msg-->错误信息，data-->null
     */
    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(HttpStatus.UNAUTHORIZED.value(), msg);
    }

    /**
     * 禁止访问
     *
     * @param msg 错误信息
     * @return code-->403，msg-->错误信息，data-->null
     */
    public static <T> Result<T> forbidden(String msg) {
        return new Result<>(HttpStatus.FORBIDDEN.value(), msg, null);
    }

    /**
     * 未找到
     *
     * @param msg 错误信息
     * @return code-->404，msg-->错误信息，data-->null
     */
    public static <T> Result<T> notFound(String msg) {
        return new Result<>(HttpStatus.NOT_FOUND.value(), msg, null);
    }

    /**
     * 冲突
     *
     * @param msg 错误信息
     * @return code-->409，msg-->错误信息，data-->null
     */
    public static <T> Result<T> conflict(String msg) {
        return new Result<>(HttpStatus.CONFLICT.value(), msg, null);
    }

    /**
     * 返回错误消息
     *
     * @param msg  返回内容
     * @param data 数据对象
     * @return code-->500，msg-->返回内容，data-->数据对象
     */
    public static <T> Result<T> error(String msg, T data) {
        return new Result<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, data);
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回内容
     * @return code-->500，msg-->返回内容，data-->null
     */
    public static <T> Result<T> error(String msg) {
        return Result.error(msg, null);
    }

    /**
     * 返回错误消息
     *
     * @param data 返回数据
     * @return code-->500，msg-->操作失败，data-->data
     */
    public static <T> Result<T> error(T data) {
        return Result.error("操作失败", data);
    }

    /**
     * 返回错误消息
     *
     * @return code-->500，msg-->操作失败，data-->null
     */
    public static <T> Result<T> error() {
        return Result.error("操作失败");
    }
}
