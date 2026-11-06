import { getAction, postAction, deleteAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';

/** 会话记录（对应后端 ai_session） */
export interface AiSessionRecord {
  id: string;
  userName: string;
  title: string;
  /** 会话绑定的知识库id列表 JSON 字符串 */
  kbIds?: string;
  createTime?: string;
  updateTime?: string;
}

/** 消息记录（对应后端 ai_message） */
export interface AiMessageRecord {
  id: string;
  sessionId: string;
  /** user 用户 / assistant 助手 */
  role: 'user' | 'assistant';
  content: string;
  /** 引用列表 JSON 字符串 */
  refs?: string;
  createTime?: string;
}

/** 引用块（SSE done 事件与 refs JSON 中的结构） */
export interface AiRefItem {
  content: string;
  score: number;
  kbId?: string;
  docId?: string;
  fileName?: string;
}

enum ApiUrl {
  /** SSE 流式问答：不走 axios，由 useAiChat 用 fetch + ReadableStream 消费 */
  CHAT = '/ai/chat',
  SESSION_LIST = '/ai/session/list',
  SESSION_MESSAGES = '/ai/session',
  SESSION_DELETE = '/ai/session',
}

/** 流式问答地址（fetch 用，需带 APP_BASE_URL 前缀） */
export const AI_CHAT_URL = ApiUrl.CHAT;

/** 当前用户会话列表（按最近活跃倒序） */
export const listSessionsApi = (): Promise<Result<AiSessionRecord[]>> => getAction(ApiUrl.SESSION_LIST);

/** 会话消息列表（按时间正序） */
export const listMessagesApi = (sessionId: string): Promise<Result<AiMessageRecord[]>> =>
  getAction(`${ApiUrl.SESSION_MESSAGES}/${sessionId}/messages`);

/** 删除会话（连同消息） */
export const deleteSessionApi = (sessionId: string): Promise<Result<null>> =>
  deleteAction(`${ApiUrl.SESSION_DELETE}/${sessionId}`);

/** 发送消息体（后端 AiChatController.ChatReq） */
export interface AiChatReq {
  sessionId?: string;
  question: string;
  kbIds?: string[];
}

/** 兜底非流式调用（一般不用，调试用） */
export const sendChatApi = (data: AiChatReq): Promise<Result<null>> => postAction(ApiUrl.CHAT, data);
