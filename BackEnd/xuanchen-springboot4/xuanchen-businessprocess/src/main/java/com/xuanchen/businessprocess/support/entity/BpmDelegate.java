package com.xuanchen.businessprocess.support.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->审批委托/代理（审批人请假期间由被委托人代为处理任务）
 *
 * <p>生效规则：status=1 且当前时间在 [start_time, end_time] 区间内（边界可空，空=不限）。
 * 任务派发时由 BpmAssigneeResolver 将委托人名下任务同步派给被委托人（或签语义）。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
@TableName("bpm_delegate")
public class BpmDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 委托人（登录名）
     */
    private String userName;

    /**
     * 被委托人（登录名）
     */
    private String delegateUserName;

    /**
     * 生效开始时间（空=立即）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 生效结束时间（空=长期）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 状态（1启用 0停用）
     */
    private Integer status;

    /**
     * 备注（如请假事由）
     */
    private String remark;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
