package com.xuanchen.businessprocess.handler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务回调注册中心-->按 business_type 收集所有 BpmBusinessHandler Bean
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Component
public class BpmHandlerRegistry {

    private final Map<String, BpmBusinessHandler> handlers = new HashMap<>();

    public BpmHandlerRegistry(List<BpmBusinessHandler> beans) {
        for (BpmBusinessHandler handler : beans) {
            handlers.put(handler.businessType(), handler);
        }
    }

    /**
     * 按业务类型取处理器，未注册返回 null（流程照常流转）
     */
    public BpmBusinessHandler get(String businessType) {
        return businessType == null ? null : handlers.get(businessType);
    }
}
