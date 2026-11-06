package com.xuanchen.businessprocess.instance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.businessprocess.instance.dto.BpmSubmitDTO;
import com.xuanchen.businessprocess.instance.entity.BpmInstance;

import java.util.Map;

/**
 * Service-->审批流程实例
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface IBpmInstanceService extends IService<BpmInstance> {

    /**
     * 发起申请
     *
     * @param userName 发起人
     * @param dto      请求
     * @return 错误原因；null 成功
     */
    String submit(String userName, BpmSubmitDTO dto);

    /**
     * 我的申请分页
     */
    IPage<BpmInstance> listMine(IPage<BpmInstance> page, String userName, Integer status, String keyword);

    /**
     * 审批历史时间线（含操作人姓名回填）
     */
    Map<String, Object> records(String instanceId);

    /**
     * 流程图渲染数据：bpmnXml + 当前节点 + 已走节点（前端 Viewer 高亮）
     */
    Map<String, Object> diagramState(String instanceId);

    /**
     * 发起人撤回
     *
     * @return 错误原因；null 成功
     */
    String revoke(String userName, String instanceId);

    /**
     * 发起人催办（WebSocket 提醒当前审批人）
     *
     * @return 错误原因；null 成功
     */
    String urge(String userName, String instanceId);
}
