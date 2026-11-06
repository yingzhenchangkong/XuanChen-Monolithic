import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import {
  AI_CHAT_URL,
  listSessionsApi,
  listMessagesApi,
  deleteSessionApi,
  type AiSessionRecord,
  type AiMessageRecord,
  type AiRefItem,
  type AiChatReq,
} from './assistant.api';
import { useAuthStore } from '@/stores';

/** 渲染用消息：历史消息 + 正在流式生成的临时消息统一成一种结构 */
export interface ChatViewMessage {
  id: string;
  role: 'user' | 'assistant';
  /** 剥离 think 后的正文（流式时随增量实时更新） */
  content: string;
  refs: AiRefItem[];
  streaming?: boolean;
}

/**
 * 剥离思考型模型（gemma4）输出的 think 段：
 * 已闭合的直接删除；未闭合（思考中）则截断到 <think> 之前，正文表现为空。
 */
export function stripThink(text: string): string {
  const closed = text.replace(/<think>[\s\S]*?<\/think>/g, '');
  const openIdx = closed.indexOf('<think>');
  const main = openIdx >= 0 ? closed.slice(0, openIdx) : closed;
  return main.trim();
}

/** 是否处于思考中（有未闭合的 <think>） */
export function isThinking(text: string): boolean {
  const openCount = (text.match(/<think>/g) || []).length;
  const closeCount = (text.match(/<\/think>/g) || []).length;
  return openCount > closeCount;
}

/**
 * AI 流式问答 hook：fetch + ReadableStream 手动解析 SSE。
 * <p>不能用 EventSource——后端 /ai/chat 是 POST 且需要 XC-ACCESS-TOKEN 头。</p>
 * <p>SSE 协议（与后端 AiChatService 对应）：增量 {"delta":"..."}、
 * 结束 {"done":true,"sessionId":"...","refs":[...]}、失败 {"error":"..."}。</p>
 */
export function useAiChat() {
  const sessions = ref<AiSessionRecord[]>([]);
  const messages = ref<ChatViewMessage[]>([]);
  /** 当前会话id（空 = 新会话，首轮回答结束后由 done 事件回填） */
  const currentSessionId = ref('');
  /** 是否正在流式接收 */
  const streaming = ref(false);
  /** 流式原始累积文本（含 think 段） */
  const rawAnswer = ref('');
  /** 流式结束后返回的引用 */
  const lastRefs = ref<AiRefItem[]>([]);
  /** 会话列表加载中 */
  const loadingSessions = ref(false);

  let abortController: AbortController | null = null;

  /** 思考中（供 UI 显示"思考中"徽标） */
  const thinking = computed(() => isThinking(rawAnswer.value));

  /** 流式中的助手消息渲染内容 */
  const streamingContent = computed(() => stripThink(rawAnswer.value));

  /** 加载会话列表 */
  async function loadSessions() {
    loadingSessions.value = true;
    try {
      const res = await listSessionsApi();
      if (res.code === 200) {
        sessions.value = res.data || [];
      }
    } finally {
      loadingSessions.value = false;
    }
  }

  /** 打开历史会话：拉取消息并渲染 */
  async function openSession(sessionId: string) {
    if (streaming.value) return;
    const res = await listMessagesApi(sessionId);
    if (res.code !== 200) {
      message.error(res.msg || '加载会话失败');
      return;
    }
    currentSessionId.value = sessionId;
    lastRefs.value = [];
    messages.value = (res.data || []).map((m: AiMessageRecord) => ({
      id: m.id,
      role: m.role,
      content: m.role === 'assistant' ? stripThink(m.content || '') : m.content || '',
      refs: parseRefs(m.refs),
    }));
  }

  /** 新建会话：清空当前视图（会话在首次提问时由后端创建） */
  function newSession() {
    if (streaming.value) return;
    currentSessionId.value = '';
    messages.value = [];
    lastRefs.value = [];
  }

  /** 删除会话 */
  async function removeSession(sessionId: string) {
    const res = await deleteSessionApi(sessionId);
    if (res.code !== 200) {
      message.error(res.msg || '删除失败');
      return;
    }
    if (sessionId === currentSessionId.value) newSession();
    await loadSessions();
  }

  /**
   * 发送问题（SSE 流式）
   */
  async function send(question: string, kbIds?: string[]) {
    const q = question.trim();
    if (!q || streaming.value) return;

    // 1、本地先渲染用户消息与空的助手占位
    messages.value.push({ id: `u-${Date.now()}`, role: 'user', content: q, refs: [] });
    messages.value.push({ id: `a-${Date.now()}`, role: 'assistant', content: '', refs: [], streaming: true });
    streaming.value = true;
    rawAnswer.value = '';
    lastRefs.value = [];

    abortController = new AbortController();
    try {
      const token = useAuthStore().getToken();
      const body: AiChatReq = { question: q, kbIds };
      if (currentSessionId.value) body.sessionId = currentSessionId.value;
      const resp = await fetch(`${import.meta.env.APP_BASE_URL}${AI_CHAT_URL}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'XC-ACCESS-TOKEN': token || '' },
        body: JSON.stringify(body),
        signal: abortController.signal,
      });
      if (!resp.ok || !resp.body) {
        // 401 等非 200 响应体是 JSON 错误，SSE 不会建立
        let msg = `请求失败（${resp.status}）`;
        try {
          const err = await resp.json();
          msg = err.msg || msg;
        } catch {
          /* 保留默认提示 */
        }
        throw new Error(msg);
      }

      // 2、逐块读取并按 SSE 事件（空行分隔）解析
      const reader = resp.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      let done = false;
      while (!done) {
        const { value, done: finished } = await reader.read();
        if (finished) break;
        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split('\n\n');
        buffer = events.pop() || '';
        for (const evt of events) {
          const line = evt.split('\n').find((l) => l.startsWith('data:'));
          if (!line) continue;
          const payload = line.slice(5).trim();
          if (!payload) continue;
          const data = JSON.parse(payload);
          if (data.delta !== undefined) {
            rawAnswer.value += data.delta;
          } else if (data.done) {
            done = true;
            lastRefs.value = data.refs || [];
            if (!currentSessionId.value && data.sessionId) {
              currentSessionId.value = data.sessionId;
              loadSessions();
            }
          } else if (data.error) {
            throw new Error(data.error);
          }
        }
      }
    } catch (e: unknown) {
      const err = e as Error;
      if (err.name === 'AbortError') {
        // 用户主动停止：保留已生成部分
        message.info('已停止生成');
      } else {
        message.error(err.message || 'AI 服务异常');
      }
    } finally {
      // 3、把流式占位转成正式消息
      const placeholder = messages.value[messages.value.length - 1];
      if (placeholder && placeholder.streaming) {
        placeholder.content = stripThink(rawAnswer.value);
        placeholder.refs = lastRefs.value;
        delete placeholder.streaming;
      }
      streaming.value = false;
      rawAnswer.value = '';
      abortController = null;
    }
  }

  /** 中断当前生成 */
  function stop() {
    abortController?.abort();
  }

  function parseRefs(refsJson?: string): AiRefItem[] {
    if (!refsJson) return [];
    try {
      return JSON.parse(refsJson);
    } catch {
      return [];
    }
  }

  return {
    sessions,
    messages,
    currentSessionId,
    streaming,
    thinking,
    streamingContent,
    rawAnswer,
    loadingSessions,
    loadSessions,
    openSession,
    newSession,
    removeSession,
    send,
    stop,
  };
}
