package com.xuanchen.common.service;

/**
 * Service接口-->日志 操作日志
 *
 * @author XuanChen
 * @date 2026-02-06
 */
public interface ILogOperationServiceCommon {
    /**
     * 记录登录日志
     */
    void recordOperationLog(String userName, String module, String ipAddress, String operationType, String requestUrl,
                            String requestParams, Boolean status, String description);
}
