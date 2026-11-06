package com.xuanchen.businessprocess.definition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->节点审批人规则
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_node_rule")
public class BpmNodeRule implements Serializable {

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
     * 节点标识
     */
    private String nodeKey;

    /**
     * 规则类型（1指定用户 2指定角色 3发起人自选）
     */
    private Integer ruleType;

    /**
     * 规则值（用户名逗号分隔/角色编码）
     */
    private String ruleValue;

    /**
     * 排序码
     */
    private Integer sortNo;
}
