package com.xuanchen.system.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VO-->工作台待办条目
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
public class DashboardTodoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待办 id
     */
    private String id;

    /**
     * 待办标题
     */
    private String title;

    /**
     * 优先级（1高 2中 3低）
     */
    private Integer priority;

    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime dueDate;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime createTime;
}
