<template>
  <!-- 大屏展示页：按编码访问 /visualization/viewer/:code，全屏铺满、等比缩放 -->
  <div class="bs-viewer">
    <ScreenRender v-if="config" :config="config" :dataset-map="datasetMap" />

    <div v-if="loading" class="bs-viewer-mask">加载中…</div>
    <div v-else-if="error" class="bs-viewer-mask is-error">{{ error }}</div>

    <div v-if="config" class="bs-viewer-toolbar">
      <span class="bs-viewer-name">{{ screenName }}</span>
      <a-button type="text" size="small" class="bs-viewer-btn" @click="toggleFullscreen">
        <template #icon>
          <FullscreenOutlined />
        </template>全屏
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { FullscreenOutlined } from '@ant-design/icons-vue';

import ScreenRender from '../design/ScreenRender.vue';
import { getScreenViewConfigApi } from '../visualization.api';
import { parseScreenConfig } from '../visualization.data';
import type { ScreenConfig } from '../visualization.types';

const route = useRoute();
const code = String(route.params.code ?? '');

const loading = ref(true);
const error = ref('');
const screenName = ref('');
const config = ref<ScreenConfig | null>(null);
const datasetMap = reactive<Record<string, { rows: Record<string, unknown>[]; columns: string[] }>>({});

onMounted(async () => {
  if (!code) {
    error.value = '缺少大屏编码';
    loading.value = false;
    return;
  }
  try {
    const data = await getScreenViewConfigApi(code);
    screenName.value = data.screenName ?? '';
    config.value = parseScreenConfig(data.configJson);
    (data.datasets ?? []).forEach((item) => {
      datasetMap[item.datasetKey] = { rows: item.rows ?? [], columns: item.columns ?? [] };
    });
  } catch (e) {
    error.value = (e as Error).message || '大屏加载失败';
  } finally {
    loading.value = false;
  }
});

const toggleFullscreen = () => {
  if (document.fullscreenElement) {
    document.exitFullscreen();
  } else {
    document.documentElement.requestFullscreen();
  }
};
</script>

<style scoped>
.bs-viewer {
  /* 同设计器：顶层路由走正常文档流铺满视口，避免 fixed 深色层残留到下一页面 */
  width: 100%;
  height: 100vh;
  background: #050a16;
  overflow: hidden;
}

.bs-viewer-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: rgba(207, 227, 255, 0.8);
  background: #050a16;
}

.bs-viewer-mask.is-error {
  color: #ff7875;
}

.bs-viewer-toolbar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  font-size: 12px;
  color: rgba(207, 227, 255, 0.35);
  opacity: 0.25;
  transition: opacity 0.2s;
}

.bs-viewer-toolbar:hover {
  opacity: 1;
}

.bs-viewer-btn {
  color: rgba(207, 227, 255, 0.75);
}
</style>
