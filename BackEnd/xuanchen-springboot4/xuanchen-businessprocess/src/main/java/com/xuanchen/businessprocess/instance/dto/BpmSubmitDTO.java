package com.xuanchen.businessprocess.instance.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO-->发起申请请求
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
public class BpmSubmitDTO {

    /**
     * 流程定义ID
     */
    private String definitionId;

    /**
     * 业务单据ID（无业务表单的通用申请可传自定义标识，如 UUID）
     */
    private String businessId;

    /**
     * 业务摘要（列表/待办标题展示）
     */
    private String businessSummary;

    /**
     * 业务表单数据（对象，条件表达式的变量来源）
     */
    private Map<String, Object> formData;

    /**
     * 发起人自选审批人（nodeKey -> 用户名列表；仅规则为"发起人自选"的节点需要）
     */
    private Map<String, List<String>> chosenAssignees;
}
