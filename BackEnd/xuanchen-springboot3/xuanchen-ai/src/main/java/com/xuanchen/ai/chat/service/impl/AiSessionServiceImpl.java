package com.xuanchen.ai.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.ai.chat.entity.AiMessage;
import com.xuanchen.ai.chat.entity.AiSession;
import com.xuanchen.ai.chat.mapper.AiSessionMapper;
import com.xuanchen.ai.chat.service.IAiMessageService;
import com.xuanchen.ai.chat.service.IAiSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 问答会话服务实现
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Service
public class AiSessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSession> implements IAiSessionService {

    @Autowired
    private IAiMessageService aiMessageService;

    @Override
    public List<AiSession> listByUser(String userName) {
        return list(new LambdaQueryWrapper<AiSession>()
                .eq(AiSession::getUserName, userName)
                .orderByDesc(AiSession::getUpdateTime)
                .orderByDesc(AiSession::getCreateTime));
    }

    @Override
    public AiSession getOwned(String sessionId, String userName) {
        AiSession session = getById(sessionId);
        if (session == null || !session.getUserName().equals(userName)) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }
        return session;
    }

    @Override
    public void deleteWithMessages(String sessionId, String userName) {
        getOwned(sessionId, userName);
        aiMessageService.remove(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getSessionId, sessionId));
        removeById(sessionId);
    }
}
