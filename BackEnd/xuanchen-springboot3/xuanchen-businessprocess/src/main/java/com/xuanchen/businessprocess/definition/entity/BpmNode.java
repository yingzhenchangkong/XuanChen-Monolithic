package com.xuanchen.businessprocess.definition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->审批流程节点
 *
 * <p>保存设计时由 BPMN XML 解析落库，供管理端查询展示；引擎运行时读取实例快照。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_node")
public class BpmNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 流程定义ID
     */
    private String definitionId;

    /**
     * 节点标识（BPMN id）
     */
    private String nodeKey;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型（1用户任务 2开始 3结束 4排他网关）
     */
    private Integer nodeType;

    /**
     * 审批方式（1或签 2会签，仅用户任务）
     */
    private Integer approvalMode;

    /**
     * 排序码
     */
    private Integer sortNo;
}
