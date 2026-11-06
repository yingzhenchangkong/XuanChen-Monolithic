<template>
  <div class="share-page">
    <a-card class="share-card" :bordered="false">
      <template #title>
        <span class="share-title">
          <FileTextOutlined /> 文件分享
        </span>
      </template>

      <!-- 加载中 -->
      <div v-if="loading" class="share-center">
        <a-spin size="large" />
      </div>

      <!-- 加载失败（不存在/已撤销/已过期） -->
      <a-result v-else-if="errorMsg" status="warning" title="无法访问该分享" :sub-title="errorMsg" />

      <!-- 分享详情 -->
      <template v-else-if="info">
        <a-descriptions :column="1" size="middle" bordered class="share-desc">
          <a-descriptions-item label="文件名">
            {{ info.name }}
          </a-descriptions-item>
          <a-descriptions-item label="大小">
            {{ formatFileSize(info.size) }}
          </a-descriptions-item>
          <a-descriptions-item label="类型">
            <a-tag color="blue">{{ (info.ext || '').toUpperCase() }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="失效时间">
            {{ info.expireTime || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <a-form layout="inline" class="share-actions" @submit.prevent>
          <a-form-item v-if="info.hasCode" label="提取码">
            <a-input v-model:value="code" placeholder="请输入提取码" :maxlength="8" style="width: 160px"
              @press-enter="handleDownload" />
          </a-form-item>
          <a-button type="primary" :loading="downloading" @click="handleDownload">
            <template #icon>
              <DownloadOutlined />
            </template>下载文件
          </a-button>
        </a-form>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { DownloadOutlined, FileTextOutlined } from '@ant-design/icons-vue';

import { formatFileSize } from '../filemanage.data';
import { getShareDownloadApi, getShareInfoApi } from '../filemanage.api';
import type { ShareInfo } from '../filemanage.api';

const route = useRoute();
const loading = ref(true);
const downloading = ref(false);
const errorMsg = ref('');
const info = ref<ShareInfo | null>(null);
const code = ref('');

const loadInfo = async () => {
  loading.value = true;
  errorMsg.value = '';
  try {
    const no = String(route.params.no || '');
    const res = await getShareInfoApi(no);
    if (res.code === 200 && res.data) {
      info.value = res.data;
    } else {
      errorMsg.value = res.msg || '分享不存在或已失效';
    }
  } catch {
    errorMsg.value = '分享信息获取失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

const handleDownload = async () => {
  if (!info.value) return;
  if (info.value.hasCode && !code.value.trim()) {
    message.warning('请输入提取码');
    return;
  }
  downloading.value = true;
  try {
    const no = String(route.params.no || '');
    const res = await getShareDownloadApi(no, code.value.trim() || undefined);
    if (res.code === 200 && res.data?.url) {
      const link = document.createElement('a');
      link.style.display = 'none';
      link.href = res.data.url;
      link.setAttribute('download', info.value.name);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } else {
      message.error(res.msg || '下载失败');
    }
  } finally {
    downloading.value = false;
  }
};

onMounted(loadInfo);
</script>

<style scoped>
.share-page {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 100vh;
  padding: 64px 16px;
  background: #f0f2f5;
}

.share-card {
  width: 100%;
  max-width: 560px;
}

.share-title {
  font-size: 16px;
}

.share-center {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.share-desc {
  margin-bottom: 20px;
}

.share-actions {
  display: flex;
  gap: 8px;
}
</style>
