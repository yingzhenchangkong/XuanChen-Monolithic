package com.xuanchen.businessprocess.task.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.businessprocess.task.entity.BpmTask;

/**
 * Service-->审批任务
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface IBpmTaskService extends IService<BpmTask> {

    /**
     * 待我审批分页
     */
    IPage<BpmTask> listTodo(IPage<BpmTask> page, String userName, String keyword);

    /**
     * 我已审批分页
     */
    IPage<BpmTask> listDone(IPage<BpmTask> page, String userName, String keyword);

    /**
     * 审批通过
     *
     * @return 错误/提示原因；null 成功
     */
    String approve(String userName, String taskId, String opinion);

    /**
     * 审批驳回
     *
     * @return 错误/提示原因；null 成功
     */
    String reject(String userName, String taskId, String opinion);

    /**
     * 转办：把待处理任务转给他人
     *
     * @return 错误/提示原因；null 成功
     */
    String transfer(String userName, String taskId, String targetUser, String opinion);

    /**
     * 驳回至指定节点（流程不办结，打回重审）
     *
     * @return 错误/提示原因；null 成功
     */
    String rejectToNode(String userName, String taskId, String targetNodeKey, String opinion);

    /**
     * 可驳回目标节点（本实例已走过的审批节点）
     */
    java.util.List<java.util.Map<String, String>> rejectTargets(String taskId);
}
