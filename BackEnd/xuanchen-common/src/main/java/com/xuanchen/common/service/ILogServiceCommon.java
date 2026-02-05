package com.xuanchen.common.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service接口-->日志 通用
 *
 * @author XuanChen
 * @date 2026-02-05
 */
public interface ILogServiceCommon {
    /**
     * 记录登录日志
     *
     * @param username    用户名
     * @param status      登录状态（true: 成功, false: 失败）
     * @param description 描述信息
     * @param request     HTTP 请求对象
     */
    void recordLoginLog(String username, Boolean status, String description, HttpServletRequest request);
}
