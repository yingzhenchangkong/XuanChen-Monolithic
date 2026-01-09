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
import { useAuthStore } from '@/stores';
import router from '@/router';

const userStore = useAuthStore();
const userInfo = userStore.getUserInfo();

const count = ref(0);

let ws: WebSocket | null = null;

const url = {
  getNoticeCount: '/system/notice/getNoticeCount',
  webSocket: 'ws://localhost:8080/ws',
}

const getNoticeCount = async () => {
  const res = await getAction(url.getNoticeCount, {});
  count.value = res.data.count;
}

const handleNoticeClick = () => {
  router.push({ name: "noticelist" });
}

// 初始化WebSocket连接
const initWebSocket = () => {
  // 创建WebSocket连接，替换为实际的WebSocket服务器地址
  ws = new WebSocket(url.webSocket);

  // 连接成功时的处理
  ws.onopen = () => {
    console.log('【websocket消息】WebSocket连接成功!');
    // 可以发送初始化消息
    ws?.send(JSON.stringify("前端和后端连接成功了!"));
    //获取当前用户未读消息数量
    getNoticeCount();
  };

  // 接收消息的处理
  ws.onmessage = (event) => {
    console.log('【websocket消息】收到WebSocket消息:', event.data);
    let rcvData = JSON.parse(event.data);
    rcvData.forEach((element: any) => {
      if (element.userName === userInfo.userName) {
        count.value = element.count;
      }
    });
  };

  // 连接错误时的处理
  ws.onerror = (error) => {
    console.error('【websocket消息】WebSocket错误:', error);
    // 可以尝试重新连接
    setTimeout(initWebSocket, 600000);
  };

  // 连接关闭时的处理
  ws.onclose = () => {
    console.log('【websocket消息】WebSocket连接已关闭,尝试重新连接!');
    // 尝试重新连接
    setTimeout(initWebSocket, 5000);
  };
};

// 组件挂载时初始化WebSocket
onMounted(() => {
  initWebSocket();
});

// 组件卸载时关闭WebSocket连接
onUnmounted(() => {
  if (ws) {
    ws.close();
    ws = null;
  }
});
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