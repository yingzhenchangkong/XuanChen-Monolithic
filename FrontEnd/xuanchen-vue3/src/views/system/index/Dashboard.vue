<template>
  <a-row :gutter="[20, 16]">
    <!-- 欢迎横幅 -->
    <a-col :span="24">
      <welcome-banner :unread-notice-count="summary?.unreadNoticeCount" :pending-todo-count="summary?.pendingTodoCount" />
    </a-col>

    <!-- 指标卡 -->
    <a-col :span="24">
      <stat-cards
        :unread-notice-count="summary?.unreadNoticeCount"
        :pending-todo-count="summary?.pendingTodoCount"
        :today-login-count="summary?.todayLoginCount"
      />
    </a-col>

    <!-- 公告 / 待办 -->
    <a-col :span="12">
      <notice-panel :notices="summary?.notices" @refresh="loadSummary" />
    </a-col>
    <a-col :span="12">
      <todo-panel :todos="summary?.todos" @refresh="loadSummary" />
    </a-col>

    <!-- 快捷入口（按用户菜单自动生成） -->
    <a-col :span="24">
      <quick-entry />
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import WelcomeBanner from './components/WelcomeBanner.vue';
import StatCards from './components/StatCards.vue';
import NoticePanel from './components/NoticePanel.vue';
import TodoPanel from './components/TodoPanel.vue';
import QuickEntry from './components/QuickEntry.vue';
import { getDashboardSummaryApi } from './index.api';
import type { DashboardSummary } from './index.types';

const summary = ref<DashboardSummary | null>(null);

const loadSummary = async () => {
  summary.value = await getDashboardSummaryApi();
};

onMounted(() => {
  loadSummary();
});

onBeforeUnmount(() => {
  summary.value = null;
});
</script>
