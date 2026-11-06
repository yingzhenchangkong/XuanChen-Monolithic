<template>
  <a-card :bordered="false" title="未读公告">
    <template #extra>
      <a-button type="link" size="small" @click="emit('refresh')">
        <ReloadOutlined />
        刷新
      </a-button>
    </template>
    <a-empty v-if="!notices?.length" description="暂无未读公告" :image-style="{ height: '48px' }" />
    <div v-else class="notice-list">
      <div v-for="notice in notices" :key="notice.noticeStatusId" class="notice-item">
        <div class="notice-main">
          <div class="notice-title" :title="notice.title">{{ notice.title }}</div>
          <div class="notice-time">{{ notice.createTime }}</div>
        </div>
        <a-popconfirm title="标记为已读？" ok-text="标记" cancel-text="取消" @confirm="handleSetRead(notice)">
          <a-button type="link" size="small">已读</a-button>
        </a-popconfirm>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import { setNoticeReadApi } from '../index.api';
import type { DashboardNotice } from '../index.types';

defineProps<{
  notices?: DashboardNotice[];
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
}>();

const marking = ref(false);

const handleSetRead = async (notice: DashboardNotice) => {
  if (marking.value) return;
  marking.value = true;
  try {
    const ok = await setNoticeReadApi(notice.noticeStatusId);
    if (ok) {
      message.success('已标记为已读');
      emit('refresh');
    } else {
      message.error('操作失败，请重试');
    }
  } finally {
    marking.value = false;
  }
};
</script>

<style lang="scss" scoped>
.notice-list {
  display: flex;
  flex-direction: column;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);

  &:last-child {
    border-bottom: none;
  }
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.notice-time {
  margin-top: 2px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
