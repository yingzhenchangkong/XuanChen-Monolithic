package com.xuanchen.businessprocess.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->审批记录（时间线）
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_record")
public class BpmRecord implements Serializable {

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
     * 操作人（sys_account.user_name）
     */
    private String operator;

    /**
     * 动作（1提交 2同意 3驳回 4撤回）
     */
    private Integer action;

    /**
     * 意见
     */
    private String opinion;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 操作人姓名（展示用，联查 sys_account.nick_name）
     */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String operatorName;
}
