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

const userStore = useAuthStore();
const userInfo = userStore.getUserInfo();
const webSocketStore = useWebSocketStore();
const count = ref(0);

const url = {
  getNoticeCount: '/system/notice/getNoticeCount',
}

const getNoticeCount = async () => {
  const res = await getAction(url.getNoticeCount, {});
  count.value = res.data.count;
}
getNoticeCount();

const handleMessage = (data: any) => {
  const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
  data.forEach((element: any) => {
    if (element.userName === userInfo.userName) {
      count.value = element.count;
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