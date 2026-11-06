package com.xuanchen.ai.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 问答会话（表 ai_session）。
 * <p>表无 create_by/update_by 审计列，仅 create_time/update_time；
 * 时间由 MybatisInterceptor 在字段存在且为空时自动填充。</p>
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Data
@TableName("ai_session")
public class AiSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属用户（关联 sys_user_account.user_name） */
    private String userName;

    /** 会话标题（默认取首条提问截断） */
    private String title;

    /** 会话绑定的知识库id列表（JSON 数组字符串，如 ["1","2"]） */
    private String kbIds;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
