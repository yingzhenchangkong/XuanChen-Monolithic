<template>
  <a-row :gutter="20">
    <a-col :span="8">
      <a-card :bordered="false" class="stat-card">
        <div class="stat-inner">
          <BellOutlined class="stat-icon" style="color: #1677ff; background-color: #e6f1fb;" />
          <div>
            <div class="stat-label">未读公告</div>
            <div class="stat-value">{{ unreadNoticeCount ?? 0 }}</div>
          </div>
        </div>
      </a-card>
    </a-col>
    <a-col :span="8">
      <a-card :bordered="false" class="stat-card">
        <div class="stat-inner">
          <CarryOutOutlined class="stat-icon" style="color: #0f6e56; background-color: #e1f5ee;" />
          <div>
            <div class="stat-label">我的待办</div>
            <div class="stat-value">{{ pendingTodoCount ?? 0 }}</div>
          </div>
        </div>
      </a-card>
    </a-col>
    <a-col :span="8">
      <a-card :bordered="false" class="stat-card">
        <div class="stat-inner">
          <LoginOutlined class="stat-icon" style="color: #854f0b; background-color: #faeeda;" />
          <div>
            <div class="stat-label">今日登录</div>
            <div class="stat-value">{{ todayLoginCount ?? 0 }}</div>
          </div>
        </div>
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
defineProps<{
  unreadNoticeCount?: number;
  pendingTodoCount?: number;
  todayLoginCount?: number;
}>();
</script>

<style lang="scss" scoped>
.stat-card {
  :deep(.ant-card-body) {
    padding: 16px 24px;
  }
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  font-size: 22px;
}

.stat-label {
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.stat-value {
  margin-top: 2px;
  font-size: 24px;
  font-weight: 500;
  line-height: 1.2;
}
</style>
