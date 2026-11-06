package com.xuanchen.ai.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.ai.chat.entity.AiMessage;
import com.xuanchen.ai.chat.mapper.AiMessageMapper;
import com.xuanchen.ai.chat.service.IAiMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 问答消息服务实现
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements IAiMessageService {

    @Override
    public List<AiMessage> listBySession(String sessionId) {
        return list(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getSessionId, sessionId)
                .orderByAsc(AiMessage::getCreateTime)
                .orderByAsc(AiMessage::getId));
    }

    @Override
    public List<AiMessage> recentMessages(String sessionId, int limit) {
        // 倒序取最近 limit 条后翻回正序，作为多轮上下文
        List<AiMessage> latest = list(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getSessionId, sessionId)
                .orderByDesc(AiMessage::getCreateTime)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + limit));
        java.util.Collections.reverse(latest);
        return latest;
    }
}
