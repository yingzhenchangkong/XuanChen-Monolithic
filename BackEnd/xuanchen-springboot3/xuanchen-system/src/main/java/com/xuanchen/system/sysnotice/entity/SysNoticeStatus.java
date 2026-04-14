package com.xuanchen.system.sysnotice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 通知 阅读状态
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Data
public class SysNoticeStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 通知编码
     */
    private String noticeId;

    /**
     * 用户
     */
    private String userId;
    @TableField(exist = false)
    private String userName;

    /**
     * 是否已读(0未读1已读)
     */
    private Boolean readStatus;
    @TableField(exist = false)
    private String readStatusName;

    /**
     * 阅读时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime readTime;

}
