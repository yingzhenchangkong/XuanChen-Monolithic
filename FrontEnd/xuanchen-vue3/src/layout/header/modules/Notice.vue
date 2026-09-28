<template>
  <a-badge :count="count" :overflowCount="99" :title="`您有${count}条消息未处理!`" :offset="[10, 15]"
    style="float: right; margin-right: 20px;">
    <span @click="handleNoticeClick">
      <BellOutlined class="icon" />
    </span>
  </a-badge>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getAction } from '@/utils/httpAction';
import { useAuthStore, useWebSocketStore } from '@/stores';
import router from '@/router';

/** WebSocket 推送的未读通知条目（后端推送载荷） */
interface WsNoticeItem {
  userName?: string;
  count?: number;
}

const userStore = useAuthStore();
const webSocketStore = useWebSocketStore();
const count = ref(0);

const url = {
  getNoticeCount: '/system/notice/getNoticeCount',
}

const getNoticeCount = async () => {
  const res = await getAction<{ count: number }>(url.getNoticeCount);
  count.value = res.data.count;
}
getNoticeCount();

const handleMessage = (data: unknown) => {
  // 直接读响应式 store 的当前用户名，不再手动解析 sessionStorage
  const currentUserName = userStore.getUserInfo()?.userName;
  if (!Array.isArray(data)) return;
  (data as WsNoticeItem[]).forEach((element) => {
    if (element.userName === currentUserName) {
      count.value = element.count ?? 0;
    }
  });
};

onMounted(() => {
  webSocketStore.createConnection(handleMessage);
});

onUnmounted(() => {
  // 组件卸载时不要关闭 WebSocket，让它保持连接
});

const handleNoticeClick = () => {
  router.push({ name: "noticelist" });
}
</script>
<style scoped>
.icon {
  float: right;
  margin-right: 15px;
  font-size: 24px;
  margin-top: 18px;
  color: #8C8C8C;
  cursor: pointer;
}
</style>