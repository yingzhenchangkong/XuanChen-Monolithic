package com.xuanchen.system.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VO-->工作台未读通知条目
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
public class DashboardNoticeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知 id
     */
    private String id;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知状态行 id（前端跳转详情/标记已读用）
     */
    private String noticeStatusId;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime createTime;
}
