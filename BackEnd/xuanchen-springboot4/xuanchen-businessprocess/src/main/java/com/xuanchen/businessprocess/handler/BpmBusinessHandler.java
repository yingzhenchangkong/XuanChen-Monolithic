package com.xuanchen.businessprocess.handler;

import com.xuanchen.businessprocess.instance.entity.BpmInstance;

/**
 * 业务回调 SPI-->业务模块接入审批流的回调接口
 *
 * <p>业务模块实现本接口并注册为 Spring Bean（businessType() 返回接入标识），
 * businessprocess 在流程关键节点回调；未注册处理器时流程正常流转，仅跳过回调。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface BpmBusinessHandler {

    /**
     * 业务类型标识（与 bpm_definition.business_type 匹配）
     */
    String businessType();

    /**
     * 流程发起后回调（实例创建成功、首个待办已生成）
     */
    default void onApply(BpmInstance instance) {
    }

    /**
     * 流程审批通过（到达结束节点）
     */
    default void onApproved(BpmInstance instance) {
    }

    /**
     * 流程被驳回
     */
    default void onRejected(BpmInstance instance) {
    }
}
