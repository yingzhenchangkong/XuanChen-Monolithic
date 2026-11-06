package com.xuanchen.ai.chat.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.ai.chat.entity.AiSession;

import java.util.List;

/**
 * AI 问答会话服务
 *
 * @author XuanChen
 * @date 2026-10-29
 */
public interface IAiSessionService extends IService<AiSession> {

    /**
     * 查询某用户的会话列表（按最近活跃倒序）
     */
    List<AiSession> listByUser(String userName);

    /**
     * 校验会话归属：存在且属于该用户则返回，否则抛业务异常
     */
    AiSession getOwned(String sessionId, String userName);

    /**
     * 删除会话及其全部消息
     */
    void deleteWithMessages(String sessionId, String userName);
}
