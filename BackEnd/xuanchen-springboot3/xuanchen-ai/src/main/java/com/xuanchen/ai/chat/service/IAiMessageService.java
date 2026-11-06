package com.xuanchen.ai.chat.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.ai.chat.entity.AiMessage;

import java.util.List;

/**
 * AI 问答消息服务
 *
 * @author XuanChen
 * @date 2026-10-29
 */
public interface IAiMessageService extends IService<AiMessage> {

    /**
     * 查询会话消息（按时间正序）
     */
    List<AiMessage> listBySession(String sessionId);

    /**
     * 取最近 N 条历史消息（时间正序返回），用于多轮上下文
     */
    List<AiMessage> recentMessages(String sessionId, int limit);
}
