<template>
  <a-card :bordered="false" class="welcome-banner">
    <div class="banner-inner">
      <a-avatar :size="48" style="background-color: #1677ff;">
        {{ avatarText }}
      </a-avatar>
      <div class="banner-text">
        <div class="greeting">{{ greeting }}，{{ nickName }}！</div>
        <div class="sub-line">今天有 {{ unreadNoticeCount }} 条未读公告、{{ pendingTodoCount }} 项待办，祝工作顺利。</div>
      </div>
      <div class="banner-date">
        <div class="date-text">{{ dateText }}</div>
        <div class="week-text">{{ weekText }}</div>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '@/stores/modules/auth';

const props = defineProps<{
  unreadNoticeCount?: number;
  pendingTodoCount?: number;
}>();

const authStore = useAuthStore();

const nickName = computed(() => authStore.getUserInfo()?.nickName || authStore.getUserInfo()?.userName || '用户');
const avatarText = computed(() => nickName.value.slice(0, 1).toUpperCase());

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 9) return '早上好';
  if (hour < 12) return '上午好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

const dateText = computed(() => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
});

const weekText = computed(() => {
  const weeks = ['日', '一', '二', '三', '四', '五', '六'];
  return `星期${weeks[new Date().getDay()]}`;
});
</script>

<style lang="scss" scoped>
.welcome-banner {
  :deep(.ant-card-body) {
    padding: 16px 24px;
  }
}

.banner-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.banner-text {
  flex: 1;
  min-width: 0;

  .greeting {
    font-size: 16px;
    font-weight: 500;
  }

  .sub-line {
    margin-top: 4px;
    color: rgba(0, 0, 0, 0.45);
    font-size: 13px;
  }
}

.banner-date {
  text-align: right;

  .date-text {
    font-size: 15px;
    font-weight: 500;
  }

  .week-text {
    margin-top: 4px;
    color: rgba(0, 0, 0, 0.45);
    font-size: 13px;
  }
}
</style>
