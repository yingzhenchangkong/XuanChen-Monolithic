package com.xuanchen.businessprocess.definition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->审批流程连线（顺序流）
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_flow")
public class BpmFlow implements Serializable {

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
     * 连线标识（BPMN id）
     */
    private String flowKey;

    /**
     * 源节点标识
     */
    private String fromNodeKey;

    /**
     * 目标节点标识
     */
    private String toNodeKey;

    /**
     * 连线名称
     */
    private String flowName;

    /**
     * 条件表达式（如 amount > 5000）
     */
    private String conditionExpr;

    /**
     * 是否默认流（网关兜底，1是 0否）
     */
    private Integer isDefault;

    /**
     * 排序码
     */
    private Integer sortNo;
}
