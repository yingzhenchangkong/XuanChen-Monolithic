package com.xuanchen.online.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类-->在线文档-思维导图
 * 数据模型：前端 simple-mind-map 产出快照 JSON 整体落库；
 * 保存为整体覆盖式（update_time 变更），逻辑删除。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
@TableName("online_mindmap")
public class OnlineMindmap implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，字符串存储）
     */
    private String id;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 思维导图快照 JSON
     */
    private String snapshot;

    /**
     * 删除状态（0正常，1已删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建人（归属人）
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
