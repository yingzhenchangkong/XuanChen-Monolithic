package com.xuanchen.businessprocess.instance.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->审批流程实例
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_instance")
public class BpmInstance implements Serializable {

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
     * 流程编码
     */
    private String definitionCode;

    /**
     * 流程名称
     */
    private String definitionName;

    /**
     * 发起时的定义版本（展示用）
     */
    private Integer version;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务单据ID
     */
    private String businessId;

    /**
     * 业务摘要（列表展示/待办标题）
     */
    private String businessSummary;

    /**
     * 发起人（sys_account.user_name）
     */
    private String applyUser;

    /**
     * 状态（1审批中 2通过 3驳回 4撤回）
     */
    private Integer status;

    /**
     * 当前节点标识（多个待处理时为最近推进节点）
     */
    private String currentNodeKey;

    /**
     * 业务表单快照 JSON（条件表达式上下文）
     */
    private String formData;

    /**
     * 图模型快照 JSON（nodes/flows/rules，发起时冻结）
     */
    private String graphSnapshot;

    /**
     * 并行网关 token 状态 JSON（{active:存活token数, arrivals:{汇聚节点:已到达数}}，办结后清空）
     */
    private String tokenState;

    /**
     * 办结时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime finishTime;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 发起人姓名（展示用，联查 sys_account.nick_name）
     */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String applyUserName;
}
