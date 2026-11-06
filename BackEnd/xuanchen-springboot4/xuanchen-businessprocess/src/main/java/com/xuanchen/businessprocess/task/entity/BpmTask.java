package com.xuanchen.businessprocess.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->审批任务
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_task")
public class BpmTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 流程实例ID
     */
    private String instanceId;

    /**
     * 节点标识
     */
    private String nodeKey;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 审批人（sys_account.user_name）
     */
    private String assignee;

    /**
     * 状态（1待处理 2已处理 3已作废）
     */
    private Integer status;

    /**
     * 处理结果（1同意 2驳回）
     */
    private Integer result;

    /**
     * 审批意见
     */
    private String opinion;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime handleTime;

    /**
     * 创建时间（=任务生成时间）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 审批人姓名（展示用，联查 sys_account.nick_name）
     */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assigneeName;

    /**
     * 业务摘要（展示用，联查 bpm_instance）
     */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String businessSummary;
}
