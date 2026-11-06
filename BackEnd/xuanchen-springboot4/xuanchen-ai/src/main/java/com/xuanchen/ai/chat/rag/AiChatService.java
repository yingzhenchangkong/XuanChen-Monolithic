package com.xuanchen.ai.chat.rag;

import com.xuanchen.ai.chat.entity.AiMessage;
import com.xuanchen.ai.chat.entity.AiSession;
import com.xuanchen.ai.chat.service.IAiMessageService;
import com.xuanchen.ai.chat.service.IAiSessionService;
import com.xuanchen.ai.config.AiProperties;
import com.xuanchen.ai.kb.rag.AiSearchService;
import com.xuanchen.common.utils.JsonUtils;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 流式问答服务（RAG）。
 * <p>链路：问题 → 向量检索（复用 AiSearchService）→ 拼引用上下文 →
 * StreamingChatModel 逐 token 回调 → SseEmitter 下发；结束后引用与答案落库。</p>
 * <p>SSE 事件协议（data 均为 JSON 字符串）：</p>
 * <ul>
 *   <li>增量：{"delta":"..."}</li>
 *   <li>结束：{"done":true,"sessionId":"...","refs":[...]}</li>
 *   <li>失败：{"error":"..."}</li>
 * </ul>
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Slf4j
@Service
public class AiChatService {

    /** 多轮上下文最多携带的历史消息条数 */
    private static final int HISTORY_LIMIT = 10;
    /** 引用块摘要截断长度（字符） */
    private static final int SNIPPET_LEN = 200;

    private final StreamingChatModel streamingChatModel;
    private final AiSearchService aiSearchService;
    private final IAiSessionService sessionService;
    private final IAiMessageService messageService;

    public AiChatService(StreamingChatModel streamingChatModel,
                         AiSearchService aiSearchService,
                         IAiSessionService sessionService,
                         IAiMessageService messageService) {
        this.streamingChatModel = streamingChatModel;
        this.aiSearchService = aiSearchService;
        this.sessionService = sessionService;
        this.messageService = messageService;
    }

    /**
     * 流式问答入口
     *
     * @param userName 当前用户
     * @param sessionId 会话id（空则新建会话）
     * @param question  用户问题
     * @param kbIds     检索的知识库范围（空 = 会话已绑定的知识库或全库）
     * @return SseEmitter
     */
    public SseEmitter chatStream(String userName, String sessionId, String question, List<String> kbIds) {
        // 1、会话：不存在则新建（标题取首条提问），存在则校验归属
        AiSession session;
        if (sessionId == null || sessionId.isBlank()) {
            session = new AiSession();
            session.setUserName(userName);
            session.setTitle(question.length() > 50 ? question.substring(0, 50) : question);
            session.setKbIds(JsonUtils.toJson(kbIds == null ? new ArrayList<String>() : kbIds));
            sessionService.save(session);
        } else {
            session = sessionService.getOwned(sessionId, userName);
        }

        // 2、检索命中的引用块（先于流式调用同步执行，约 1 秒）
        List<String> scope = kbIds != null && !kbIds.isEmpty() ? kbIds : parseKbIds(session.getKbIds());
        List<Map<String, Object>> refs = aiSearchService.search(question, scope);

        // 3、构建多轮消息列表：系统提示 + 最近历史 + 本轮问题
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(dev.langchain4j.data.message.SystemMessage.from(buildSystemPrompt(refs)));
        for (AiMessage history : messageService.recentMessages(session.getId(), HISTORY_LIMIT)) {
            if ("user".equals(history.getRole())) {
                messages.add(dev.langchain4j.data.message.UserMessage.from(history.getContent()));
            } else if ("assistant".equals(history.getRole())) {
                messages.add(dev.langchain4j.data.message.AiMessage.from(history.getContent()));
            }
        }
        messages.add(dev.langchain4j.data.message.UserMessage.from(question));

        // 4、用户消息先落库
        AiMessage userMsg = new AiMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("user");
        userMsg.setContent(question);
        messageService.save(userMsg);

        // 5、流式调用与 SSE 下发
        SseEmitter emitter = new SseEmitter(0L);
        StringBuilder answer = new StringBuilder();
        streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                try {
                    emitter.send(SseEmitter.event().data(sse("delta", token)));
                    answer.append(token);
                } catch (Exception e) {
                    log.warn("SSE 发送失败（客户端可能已断开）：{}", e.getMessage());
                }
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                try {
                    String full = stripThink(answer.toString());
                    // 助手消息 + 引用落库
                    AiMessage aiMsg = new AiMessage();
                    aiMsg.setSessionId(session.getId());
                    aiMsg.setRole("assistant");
                    aiMsg.setContent(full);
                    aiMsg.setRefs(JsonUtils.toJson(refs));
                    messageService.save(aiMsg);
                    // 触碰更新时间，会话列表按活跃度排序
                    session.setUpdateTime(java.time.LocalDateTime.now());
                    sessionService.updateById(session);
                    // 结束事件
                    Map<String, Object> end = new LinkedHashMap<>();
                    end.put("done", true);
                    end.put("sessionId", session.getId());
                    end.put("refs", refs);
                    emitter.send(SseEmitter.event().data(JsonUtils.toJson(end)));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("问答收尾失败", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式问答失败：{}", error.getMessage(), error);
                try {
                    emitter.send(SseEmitter.event().data(sse("error",
                            "AI 服务异常，请稍后重试：" + error.getMessage())));
                } catch (Exception ignore) {
                    // 客户端已断开
                }
                emitter.completeWithError(error);
            }
        });
        return emitter;
    }

    /**
     * 系统提示词：角色设定 + 检索到的知识库上下文（带引用编号）
     */
    private String buildSystemPrompt(List<Map<String, Object>> refs) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是萱晨管理系统的智能助手，用简体中文回答，回答使用 Markdown 格式。");
        if (refs == null || refs.isEmpty()) {
            sb.append("当前没有检索到知识库资料，请基于通用知识谨慎回答，并提示用户可在知识库中补充相关文档。");
            return sb.toString();
        }
        sb.append("请主要依据下面编号的知识库片段回答用户问题；")
          .append("引用信息时在句末标注对应编号（如 [1]）；片段中没有的内容不要编造。\n\n");
        for (int i = 0; i < refs.size(); i++) {
            Map<String, Object> ref = refs.get(i);
            String content = String.valueOf(ref.get("content"));
            if (content.length() > SNIPPET_LEN * 3) {
                content = content.substring(0, SNIPPET_LEN * 3);
            }
            sb.append("[").append(i + 1).append("] 来源：")
              .append(ref.get("fileName")).append("\n")
              .append(content).append("\n\n");
        }
        return sb.toString();
    }

    /** 去掉思考型模型（gemma4）输出的 think 段，只保留正式答案落库 */
    private String stripThink(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseKbIds(String kbIdsJson) {
        if (kbIdsJson == null || kbIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        // 用原始 List 类型反序列化，避开 Jackson 2/3 TypeReference 双版本差异
        return JsonUtils.fromJson(kbIdsJson, List.class);
    }

    /** 构造单键 SSE 事件 JSON */
    private String sse(String key, String value) {
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put(key, value);
        return JsonUtils.toJson(map);
    }
}
