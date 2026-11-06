package com.xuanchen.monitor.logoperation.service.impl;

import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.service.ILogOperationServiceCommon;
import com.xuanchen.monitor.logoperation.entity.LogOperation;
import com.xuanchen.monitor.logoperation.service.ILogOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service接口实现类-->日志 操作
 *
 * @author XuanChen
 * @date 2026-02-07
 */
@Service
public class LogOperationServiceCommonImpl implements ILogOperationServiceCommon {
    @Autowired
    private ILogOperationService logOperationService;

    @Override
    public void recordOperationLog(String userName, String module, String ipAddress, String operationType, String requestUrl,
                                   String requestParams, Boolean status, String description) {
        LogOperation logOperation = new LogOperation();
        logOperation.setUserName(userName);
        logOperation.setModule(module);
        logOperation.setIpAddress(ipAddress);
        logOperation.setOperationType(operationType);
        logOperation.setRequestUrl(requestUrl);
        logOperation.setRequestParams(requestParams);
        logOperation.setStatus(Boolean.TRUE.equals(status) ? CommonConst.STATUS_ENABLED : CommonConst.STATUS_DISABLED); // 操作状态（1成功，0失败）
        logOperation.setDescription(description);
        logOperation.setOperationTime(LocalDateTime.now());
        logOperationService.save(logOperation);
    }
}

