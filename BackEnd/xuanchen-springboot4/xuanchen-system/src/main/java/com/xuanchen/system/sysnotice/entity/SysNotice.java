package com.xuanchen.system.sysnotice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实体类 --> 通知
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Data
public class SysNotice implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 状态
     */
    private Integer status;
    @TableField(exist = false)
    private String statusName;

    /**
     * 创建人
     */
    private String createBy;
    @TableField(exist = false)
    private String createName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;
    @TableField(exist = false)
    private String updateName;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 接收用户(单)
     */
    @TableField(exist = false)
    private String userId;
    @TableField(exist = false)
    private String userName;

    /**
     * 接收用户(多)
     */
    @TableField(exist = false)
    private List<String> listUser;

    /**
     * 阅读状态
     */
    @TableField(exist = false)
    private String noticeStatusId;
    @TableField(exist = false)
    private Boolean readStatus;
    @TableField(exist = false)
    private String readStatusName;

    /**
     * 阅读时间
     */
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String readTime;
}
