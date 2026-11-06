<template>
  <!-- 悬浮球：可拖拽（位移 < 6px 视为点击） -->
  <div v-if="!visible" class="xc-ai-fab" :style="{ left: `${fabPos.x}px`, top: `${fabPos.y}px` }"
    @pointerdown="onFabDown">
    <RobotOutlined />
    <span class="xc-ai-fab-tip">AI 助手</span>
  </div>

  <!-- 对话抽屉 -->
  <a-drawer v-model:open="visible" placement="right" :width="480" class="xc-ai-drawer"
    :body-style="{ padding: 0, display: 'flex', flexDirection: 'column', height: '100%' }"
    @after-open-change="onDrawerOpen">
    <template #title>
      <div class="xc-ai-head">
        <span class="xc-ai-head-title">
          <RobotOutlined /> AI 助手
        </span>
        <a-space :size="4">
          <a-button type="link" size="small" @click="newSession">
            <PlusOutlined /> 新会话
          </a-button>
          <a-dropdown :trigger="['click']">
            <a-button type="link" size="small">
              <HistoryOutlined /> 历史
            </a-button>
            <template #overlay>
              <a-menu style="max-height: 320px; overflow: auto; min-width: 220px">
                <a-empty v-if="!sessions.length" description="暂无历史会话" :image-style="{ height: '48px' }" />
                <a-menu-item v-for="s in sessions" :key="s.id" @click="openSession(s.id)">
                  <div class="xc-ai-session-item">
                    <span class="xc-ai-session-title">{{ s.title || '未命名会话' }}</span>
                    <DeleteOutlined class="xc-ai-session-del" @click.stop="delSession(s.id)" />
                  </div>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </div>
    </template>

    <!-- 消息列表 -->
    <div ref="listRef" class="xc-ai-list">
      <div v-if="!viewMessages.length" class="xc-ai-empty">
        <RobotOutlined class="xc-ai-empty-icon" />
        <p>你好，我是萱晨 AI 助手</p>
        <p class="xc-ai-empty-sub">基于知识库回答问题，支持 Markdown；发送第一条消息开始对话</p>
      </div>

      <div v-for="msg in viewMessages" :key="msg.id" class="xc-ai-msg" :class="msg.role">
        <div class="xc-ai-bubble" :class="msg.role">
          <!-- 用户消息原样展示；助手消息 Markdown 渲染 -->
          <template v-if="msg.role === 'user'">{{ msg.content }}</template>
          <template v-else>
            <div v-if="msg.streaming && thinking" class="xc-ai-thinking">
              <LoadingOutlined /> 思考中…
            </div>
            <div v-if="msg.content" class="xc-ai-md" v-html="renderMd(msg.content)"></div>
            <div v-else-if="msg.streaming && !thinking" class="xc-ai-cursor">▍</div>
            <div v-if="msg.streaming && msg.content" class="xc-ai-cursor">▍</div>
            <!-- 引用来源 -->
            <div v-if="msg.refs.length && !msg.streaming" class="xc-ai-refs">
              <div class="xc-ai-refs-title">
                <BookOutlined /> 参考来源
              </div>
              <a-tag v-for="(ref, i) in msg.refs" :key="i" class="xc-ai-ref-tag" :title="ref.content">
                [{{ i + 1 }}] {{ ref.fileName || '未知文档' }}
              </a-tag>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="xc-ai-input">
      <a-textarea v-model:value="input" placeholder="输入问题，Enter 发送，Shift+Enter 换行"
        :auto-size="{ minRows: 1, maxRows: 4 }" :disabled="streaming" @keydown.enter.exact.prevent="onSend" />
      <a-button v-if="!streaming" type="primary" :disabled="!input.trim()" @click="onSend">
        <SendOutlined />
      </a-button>
      <a-button v-else danger @click="stop">
        <PauseCircleOutlined /> 停止
      </a-button>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
import { ref, computed, nextTick, watch } from 'vue';
import MarkdownIt from 'markdown-it';
import { useAiChat } from '@/views/ai/assistant/useAiChat';

const md = new MarkdownIt({ html: false, linkify: true, breaks: true });

/** Markdown 安全渲染（html:false 防 XSS） */
function renderMd(content: string): string {
  return md.render(content || '');
}

const {
  sessions,
  messages,
  streaming,
  thinking,
  streamingContent,
  loadSessions,
  openSession,
  newSession,
  removeSession,
  send,
  stop,
} = useAiChat();

const visible = ref(false);
const input = ref('');
const listRef = ref<HTMLDivElement>();

/** 悬浮球位置（右下角默认） */
const fabPos = ref({ x: window.innerWidth - 88, y: window.innerHeight - 120 });
let dragging = false;
let moved = false;
let startX = 0;
let startY = 0;

function onFabDown(e: PointerEvent) {
  dragging = true;
  moved = false;
  startX = e.clientX - fabPos.value.x;
  startY = e.clientY - fabPos.value.y;
  window.addEventListener('pointermove', onFabMove);
  window.addEventListener('pointerup', onFabUp, { once: true });
}

function onFabMove(e: PointerEvent) {
  if (!dragging) return;
  const x = e.clientX - startX;
  const y = e.clientY - startY;
  if (Math.abs(x - fabPos.value.x) > 6 || Math.abs(y - fabPos.value.y) > 6) moved = true;
  fabPos.value = {
    x: Math.min(Math.max(8, x), window.innerWidth - 72),
    y: Math.min(Math.max(8, y), window.innerHeight - 72),
  };
}

function onFabUp() {
  dragging = false;
  window.removeEventListener('pointermove', onFabMove);
  if (!moved) openDrawer();
}

function openDrawer() {
  visible.value = true;
}

/** 首次打开时加载会话列表 */
function onDrawerOpen(open: boolean) {
  if (open) {
    loadSessions();
    scrollBottom();
  }
}

/** 视图消息：历史消息 + 流式中的临时消息（占位最后一条） */
const viewMessages = computed(() => {
  if (!streaming.value) return messages.value;
  const list = messages.value.slice(0, -1);
  const placeholder = messages.value[messages.value.length - 1];
  return [
    ...list,
    { ...placeholder, content: streamingContent.value, refs: [] },
  ];
});

function onSend() {
  const q = input.value.trim();
  if (!q || streaming.value) return;
  input.value = '';
  send(q);
  scrollBottom();
}

function delSession(id: string) {
  removeSession(id);
}

// 流式增量 / 消息变化时跟随滚动到底部
watch(
  () => streamingContent.value.length + messages.value.length,
  () => scrollBottom(),
);

async function scrollBottom() {
  await nextTick();
  const el = listRef.value;
  if (el) el.scrollTop = el.scrollHeight;
}
</script>

<style scoped>
.xc-ai-fab {
  position: fixed;
  z-index: 999;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f7cff, #6a5cff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(79, 124, 255, 0.45);
  transition: transform 0.2s;
  user-select: none;
  touch-action: none;
}

.xc-ai-fab:hover {
  transform: scale(1.08);
}

.xc-ai-fab-tip {
  position: absolute;
  bottom: -18px;
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.xc-ai-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.xc-ai-head-title {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.xc-ai-session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.xc-ai-session-title {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.xc-ai-session-del {
  color: #999;
}

.xc-ai-session-del:hover {
  color: #ff4d4f;
}

.xc-ai-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

.xc-ai-empty {
  text-align: center;
  color: #8c8c8c;
  padding-top: 80px;
}

.xc-ai-empty-icon {
  font-size: 42px;
  color: #4f7cff;
  margin-bottom: 12px;
}

.xc-ai-empty-sub {
  font-size: 12px;
}

.xc-ai-msg {
  display: flex;
  margin-bottom: 14px;
}

.xc-ai-msg.user {
  justify-content: flex-end;
}

.xc-ai-bubble {
  max-width: 86%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}

.xc-ai-bubble.user {
  background: #4f7cff;
  color: #fff;
}

.xc-ai-bubble.assistant {
  background: #fff;
  border: 1px solid #e8eaee;
}

.xc-ai-thinking {
  color: #4f7cff;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.xc-ai-cursor {
  display: inline;
  color: #4f7cff;
  animation: xc-ai-blink 1s step-start infinite;
}

@keyframes xc-ai-blink {
  50% {
    opacity: 0;
  }
}

.xc-ai-refs {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #e8eaee;
}

.xc-ai-refs-title {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.xc-ai-ref-tag {
  cursor: default;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.xc-ai-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #e8eaee;
  background: #fff;
}

/* Markdown 内容基础样式（scoped 下用 :deep 穿透 v-html） */
.xc-ai-md :deep(p) {
  margin: 0 0 8px;
}

.xc-ai-md :deep(p:last-child) {
  margin-bottom: 0;
}

.xc-ai-md :deep(pre) {
  background: #f0f2f5;
  padding: 10px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
}

.xc-ai-md :deep(code) {
  background: #f0f2f5;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 13px;
}

.xc-ai-md :deep(pre code) {
  background: none;
  padding: 0;
}

.xc-ai-md :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
}

.xc-ai-md :deep(th),
.xc-ai-md :deep(td) {
  border: 1px solid #e0e2e8;
  padding: 4px 10px;
  font-size: 13px;
}

.xc-ai-md :deep(ul),
.xc-ai-md :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}
</style>
