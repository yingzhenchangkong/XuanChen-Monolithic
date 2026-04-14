package com.xuanchen.common.entity;

import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 实体类-->通用返回结果
 *
 * @author XuanChen
 * @date 2025-03-05
 */
public class Result extends HashMap<String, Object> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 状态码
     */
    public static final String CODE_TAG = "code";
    /**
     * 返回内容
     */
    public static final String MSG_TAG = "msg";
    /**
     * 数据对象
     */
    public static final String DATA_TAG = "data";

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
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    /**
     * 构造函数 (状态码，返回内容，数据对象)
     *
     * @param code 状态码
     * @param msg  返回内容
     * @param data 数据对象
     */
    public Result(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (data != null) {
            super.put(DATA_TAG, data);
        }
    }

    /**
     * 返回成功消息
     *
     * @param msg  返回内容
     * @param data 数据对象
     * @return code-->200，msg-->返回内容，data-->数据对象
     */
    public static Result success(String msg, Object data) {
        return new Result(HttpStatus.OK.value(), msg, data);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回内容
     * @return code-->200，msg-->返回内容，data-->null
     */
    public static Result success(String msg) {
        return Result.success(msg, null);
    }

    /**
     * 返回成功消息
     *
     * @param data 返回数据
     * @return code-->200，msg-->操作成功，data-->data
     */
    public static Result success(Object data) {
        return Result.success("操作成功", data);
    }

    /**
     * 返回成功消息
     *
     * @return code-->200，msg-->操作成功，data-->null
     */
    public static Result success() {
        return Result.success("操作成功");
    }

    /**
     * 返回错误消息
     *
     * @param msg  返回内容
     * @param data 数据对象
     * @return code-->500，msg-->返回内容，data-->数据对象
     */
    public static Result error(String msg, Object data) {
        return new Result(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, data);
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回内容
     * @return code-->500，msg-->返回内容，data-->null
     */
    public static Result error(String msg) {
        return Result.error(msg, null);
    }

    /**
     * 返回错误消息
     *
     * @param data 返回数据
     * @return code-->500，msg-->操作失败，data-->data
     */
    public static Result error(Object data) {
        return Result.error("操作失败", data);
    }

    /**
     * 返回错误消息
     *
     * @return code-->500，msg-->操作失败，data-->null
     */
    public static Result error() {
        return Result.error("操作失败");
    }

}
