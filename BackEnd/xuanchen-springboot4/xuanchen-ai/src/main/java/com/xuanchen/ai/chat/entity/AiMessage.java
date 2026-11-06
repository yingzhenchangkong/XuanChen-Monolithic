package com.xuanchen.ai.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 问答消息（表 ai_message）。
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Data
@TableName("ai_message")
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属会话id */
    private String sessionId;

    /** 角色（user 用户 / assistant 助手） */
    private String role;

    /** 消息内容（Markdown） */
    private String content;

    /** 引用列表（JSON 数组字符串：[{fileName,docId,score,snippet}]） */
    private String refs;

    /** 创建时间 */
    private LocalDateTime createTime;
}
