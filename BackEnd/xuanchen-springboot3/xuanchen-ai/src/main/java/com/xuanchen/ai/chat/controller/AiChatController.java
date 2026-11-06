package com.xuanchen.ai.chat.controller;

import com.xuanchen.ai.chat.entity.AiMessage;
import com.xuanchen.ai.chat.entity.AiSession;
import com.xuanchen.ai.chat.rag.AiChatService;
import com.xuanchen.ai.chat.service.IAiMessageService;
import com.xuanchen.ai.chat.service.IAiSessionService;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 流式问答控制器。
 * <p>POST /ai/chat 返回 SseEmitter：响应头 text/event-stream，
 * 前端必须用 fetch + ReadableStream 消费（EventSource 不支持 POST 与自定义鉴权头）。</p>
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final IAiSessionService sessionService;
    private final IAiMessageService messageService;
    private final IAuthServiceCommon authServiceCommon;

    public AiChatController(AiChatService aiChatService,
                            IAiSessionService sessionService,
                            IAiMessageService messageService,
                            IAuthServiceCommon authServiceCommon) {
        this.aiChatService = aiChatService;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.authServiceCommon = authServiceCommon;
    }

    /** 问答请求体 */
    public static class ChatReq {
        /** 会话id（空 = 新建会话） */
        private String sessionId;
        /** 用户问题 */
        private String question;
        /** 检索的知识库范围（空 = 会话绑定范围或全库） */
        private List<String> kbIds;

        public String getSessionId() {
            return sessionId;
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getKbIds() {
            return kbIds;
        }
    }

    /**
     * 流式问答（SSE）
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatReq req) {
        String question = req.getQuestion();
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        String userName = authServiceCommon.getCurrentUserName();
        return aiChatService.chatStream(userName, req.getSessionId(), question.trim(), req.getKbIds());
    }

    /**
     * 当前用户会话列表（按最近活跃倒序）
     */
    @GetMapping("/session/list")
    public Result<List<AiSession>> sessionList() {
        String userName = authServiceCommon.getCurrentUserName();
        return Result.success(sessionService.listByUser(userName));
    }

    /**
     * 会话消息列表（按时间正序）
     */
    @GetMapping("/session/{id}/messages")
    public Result<List<AiMessage>> messages(@PathVariable("id") String sessionId) {
        String userName = authServiceCommon.getCurrentUserName();
        sessionService.getOwned(sessionId, userName);
        return Result.success(messageService.listBySession(sessionId));
    }

    /**
     * 删除会话（连同消息）
     */
    @DeleteMapping("/session/{id}")
    public Result<String> deleteSession(@PathVariable("id") String sessionId) {
        String userName = authServiceCommon.getCurrentUserName();
        sessionService.deleteWithMessages(sessionId, userName);
        return Result.success("删除成功");
    }
}
