package com.xuanchen.monitor.monlogoperation.service.impl;

import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.service.ILogOperationServiceCommon;
import com.xuanchen.monitor.monlogoperation.entity.MonLogOperation;
import com.xuanchen.monitor.monlogoperation.service.IMonLogOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service接口实现类-->日志 操作
 *
 * @author XuanChen
 * @date 2026-02-07
 */
@Service
@RequiredArgsConstructor
public class LogOperationServiceCommonImpl implements ILogOperationServiceCommon {
    private final IMonLogOperationService monLogOperationService;

    @Override
    public void recordOperationLog(String userName, String module, String ipAddress, String operationType, String requestUrl,
                                   String requestParams, Boolean status, String description) {
        MonLogOperation monLogOperation = new MonLogOperation();
        monLogOperation.setUserName(userName);
        monLogOperation.setModule(module);
        monLogOperation.setIpAddress(ipAddress);
        monLogOperation.setOperationType(operationType);
        monLogOperation.setRequestUrl(requestUrl);
        monLogOperation.setRequestParams(requestParams);
        monLogOperation.setStatus(Boolean.TRUE.equals(status) ? CommonConst.STATUS_ENABLED : CommonConst.STATUS_DISABLED);
        monLogOperation.setDescription(description);
        monLogOperation.setOperationTime(LocalDateTime.now());
        monLogOperationService.save(monLogOperation);
    }
}
