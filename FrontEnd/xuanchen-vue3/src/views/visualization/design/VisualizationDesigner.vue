<template>
  <!-- 大屏设计器：全屏独立页面（不走后台 Layout），左组件 / 中画布 / 右属性 -->
  <!-- 暗色主题由 App.vue 根 ConfigProvider 按路由统一提供（局部 darkAlgorithm Provider
       卸载后会污染全局主题上下文，导致返回列表页整体变黑），此处不再嵌套 Provider -->
  <div class="bs-designer">
      <header class="bs-designer-header">
        <div class="bs-header-left">
          <a-button type="text" class="bs-back" @click="goBack">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
          </a-button>
          <div class="bs-title">
            <span class="bs-name">{{ screen.screenName || '未命名大屏' }}</span>
            <span class="bs-code">{{ screen.screenCode }}</span>
          </div>
          <a-tag :color="screenStatusColor(screen.status)">{{ screenStatusText(screen.status) }}</a-tag>
          <a-tag v-if="dirty" color="warning">未保存</a-tag>
        </div>
        <div class="bs-header-actions">
          <span class="bs-hint">拖拽组件到画布 · 拖动移动 / 四角缩放 · Ctrl+S 保存 · Delete 删除</span>
          <a-button @click="datasetOpen = true">
            <template #icon>
              <DatabaseOutlined />
            </template>数据集
          </a-button>
          <a-button @click="openPreview">
            <template #icon>
              <EyeOutlined />
            </template>预览
          </a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">
            <template #icon>
              <SaveOutlined />
            </template>保存
          </a-button>
        </div>
      </header>

      <div class="bs-designer-body">
        <WidgetPalette @add="handleAddWidget" />
        <CanvasStage :config="config" :selected-id="selectedId" :dataset-map="datasetMap" @select="handleSelect"
          @add-widget="handleAddWidgetAt" @change="markDirty" />
        <PropertyPanel :widget="selectedWidget" :canvas="config.canvas" :datasets="datasets"
          :dataset-cache="datasetMap" :loading-key="loadingKey" @change="markDirty" @delete="handleDeleteWidget"
          @to-front="handleToFront" @to-back="handleToBack" @load-data="handleLoadData"
          @manage-dataset="datasetOpen = true" />
      </div>

      <DatasetModal v-model:open="datasetOpen" :screen-id="id" :datasets="datasets"
        @change="handleDatasetChange" @data-loaded="handleDataLoaded" />

      <!-- 预览：直接复用只读画布，草稿也能看效果 -->
      <div v-if="previewOpen" class="bs-preview-layer">
        <div class="bs-preview-bar">
          <span>{{ screen.screenName }}（预览）</span>
          <a-button size="small" @click="previewOpen = false">关闭预览</a-button>
        </div>
        <div class="bs-preview-body">
          <ScreenRender :config="config" :dataset-map="datasetMap" />
        </div>
      </div>

      <div v-if="loading" class="bs-loading">加载中…</div>
    </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  ArrowLeftOutlined, DatabaseOutlined, EyeOutlined, SaveOutlined,
} from '@ant-design/icons-vue';

import WidgetPalette from './components/WidgetPalette.vue';
import CanvasStage from './components/CanvasStage.vue';
import PropertyPanel from './components/PropertyPanel.vue';
import DatasetModal from './modal/DatasetModal.vue';
import ScreenRender from './ScreenRender.vue';
import { createWidget } from './widgetCatalog';
import { getScreenConfigApi, previewScreenDatasetApi, saveScreenConfigApi } from '../visualization.api';
import { parseScreenConfig, screenStatusColor, screenStatusText } from '../visualization.data';
import type {
  VisualizationRecord, ScreenConfig, ScreenDataset, ScreenWidget, WidgetType,
} from '../visualization.types';

const route = useRoute();
const router = useRouter();
const id = String(route.params.id ?? '');

const loading = ref(true);
const saving = ref(false);
const dirty = ref(false);
const datasetOpen = ref(false);
const previewOpen = ref(false);
const loadingKey = ref('');

const screen = reactive<VisualizationRecord>({ screenName: '', screenCode: '', status: 0 });
const config = reactive<ScreenConfig>({ canvas: parseScreenConfig().canvas, widgets: [] });
const datasets = ref<ScreenDataset[]>([]);
/** 数据集取数缓存：datasetKey -> 行/列，画布与预览共用 */
const datasetMap = reactive<Record<string, { rows: Record<string, unknown>[]; columns: string[] }>>({});
const selectedId = ref('');

const selectedWidget = computed<ScreenWidget | null>(
  () => config.widgets.find((widget) => widget.id === selectedId.value) ?? null,
);

const markDirty = () => {
  dirty.value = true;
};

const nextZIndex = () => {
  const max = config.widgets.reduce((acc, widget) => Math.max(acc, widget.zIndex ?? 0), 0);
  return max + 1;
};

const handleSelect = (widgetId: string | null) => {
  selectedId.value = widgetId ?? '';
};

/** 点击左侧组件：放在画布中央（多组件时逐级偏移，避免完全重叠） */
const handleAddWidget = (type: WidgetType) => {
  const canvas = config.canvas;
  const offset = (config.widgets.length % 6) * 24;
  const temp = createWidget(type, 0, 0, nextZIndex());
  const x = Math.max(0, (canvas.width - temp.w) / 2 + offset);
  const y = Math.max(0, (canvas.height - temp.h) / 2 + offset);
  addWidget(type, x, y);
};

/** 从面板拖入：使用落点坐标 */
const handleAddWidgetAt = (payload: { type: WidgetType; x: number; y: number }) => {
  addWidget(payload.type, payload.x, payload.y);
};

const addWidget = (type: WidgetType, x: number, y: number) => {
  const widget = createWidget(type, x, y, nextZIndex());
  config.widgets.push(widget);
  selectedId.value = widget.id;
  markDirty();
};

const handleDeleteWidget = () => {
  if (!selectedId.value) return;
  config.widgets = config.widgets.filter((widget) => widget.id !== selectedId.value);
  selectedId.value = '';
  markDirty();
};

const handleToFront = () => {
  if (!selectedWidget.value) return;
  selectedWidget.value.zIndex = nextZIndex();
  markDirty();
};

const handleToBack = () => {
  if (!selectedWidget.value) return;
  const min = config.widgets.reduce((acc, widget) => Math.min(acc, widget.zIndex ?? 1), 1);
  selectedWidget.value.zIndex = Math.max(1, min - 1);
  markDirty();
};

/** 单个数据集加载（属性面板「加载预览数据」） */
const handleLoadData = async (datasetKey?: string) => {
  if (!datasetKey) {
    message.warning('请先选择数据集');
    return;
  }
  const dataset = datasets.value.find((item) => item.datasetKey === datasetKey);
  if (!dataset?.id) {
    message.warning('该数据集尚未保存，请先在「数据集」中保存后再加载');
    return;
  }
  loadingKey.value = datasetKey;
  try {
    const res = await previewScreenDatasetApi({ datasetId: String(dataset.id) });
    if (res.code !== 200 || !res.data) {
      message.error(res.msg || '数据集执行失败');
      return;
    }
    datasetMap[datasetKey] = { rows: res.data.rows ?? [], columns: res.data.columns ?? [] };
    message.success(`已加载 ${res.data.total ?? 0} 行数据`);
  } catch (e) {
    message.error((e as Error).message || '数据集执行失败');
  } finally {
    loadingKey.value = '';
  }
};

const handleDataLoaded = (payload: { datasetKey: string; columns: string[]; rows: Record<string, unknown>[] }) => {
  datasetMap[payload.datasetKey] = { rows: payload.rows, columns: payload.columns };
};

const handleDatasetChange = (list: ScreenDataset[]) => {
  datasets.value = list;
  markDirty();
};

/** 打开设计器时预加载所有数据集，让图表直接出图（失败不阻塞页面） */
const preloadDatasets = async () => {
  const tasks = datasets.value
    .filter((item) => item.id)
    .slice(0, 10)
    .map(async (item) => {
      const key = String(item.datasetKey ?? '');
      if (!key) return;
      try {
        const res = await previewScreenDatasetApi({ datasetId: String(item.id) });
        if (res.code === 200 && res.data) {
          datasetMap[key] = { rows: res.data.rows ?? [], columns: res.data.columns ?? [] };
        }
      } catch {
        // 单个数据集失败只影响对应组件（画布显示「暂无数据」），不打断设计器初始化
      }
    });
  await Promise.allSettled(tasks);
};

const handleSave = async () => {
  if (!id) return;
  saving.value = true;
  try {
    const res = await saveScreenConfigApi(id, JSON.stringify(config));
    if (res.code !== 200) {
      message.error(res.msg || '保存失败');
      return;
    }
    dirty.value = false;
    message.success('保存成功！');
  } catch (e) {
    message.error((e as Error).message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const openPreview = () => {
  if (!config.widgets.length) {
    message.warning('画布还没有组件');
    return;
  }
  previewOpen.value = true;
};

const goBack = () => {
  if (dirty.value) {
    // eslint-disable-next-line no-alert
    if (!window.confirm('有未保存的修改，确定离开吗？')) return;
  }
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push({ name: 'visualization' });
  }
};

const handleKeydown = (event: KeyboardEvent) => {
  const target = event.target as HTMLElement | null;
  const tag = target?.tagName?.toLowerCase();
  if (tag === 'input' || tag === 'textarea' || target?.isContentEditable) return;

  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault();
    handleSave();
    return;
  }
  if ((event.key === 'Delete' || event.key === 'Backspace') && selectedId.value) {
    event.preventDefault();
    handleDeleteWidget();
  }
  if (event.key === 'Escape') {
    selectedId.value = '';
    previewOpen.value = false;
  }
};

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!dirty.value) return;
  event.preventDefault();
  event.returnValue = '';
};

onMounted(async () => {
  if (!id) {
    message.error('缺少大屏ID');
    loading.value = false;
    return;
  }
  try {
    const data = await getScreenConfigApi(id);
    Object.assign(screen, data.screen ?? {});
    const parsed = parseScreenConfig(data.screen?.configJson);
    config.canvas = parsed.canvas;
    config.widgets = parsed.widgets;
    datasets.value = data.datasets ?? [];
    await preloadDatasets();
  } catch (e) {
    message.error((e as Error).message || '加载大屏失败');
  } finally {
    loading.value = false;
  }
  window.addEventListener('keydown', handleKeydown);
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
  window.removeEventListener('beforeunload', handleBeforeUnload);
});
</script>

<style scoped>
/* 顶层路由用正常文档流铺满视口：视觉与最初的全屏深色页一致，
   且相比 position:fixed 覆盖层不会把深色残留带到列表页 */
.bs-designer {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #070d18;
  color: #e6f0ff;
}

.bs-designer-header {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #0d1729;
  border-bottom: 1px solid rgba(64, 169, 255, 0.18);
}

.bs-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bs-back {
  color: #cfe3ff;
}

.bs-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.bs-name {
  font-size: 16px;
  font-weight: 600;
}

.bs-code {
  font-size: 12px;
  opacity: 0.55;
}

.bs-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bs-hint {
  font-size: 12px;
  opacity: 0.45;
  margin-right: 6px;
}

.bs-designer-body {
  flex: 1;
  min-height: 0;
  display: flex;
}

.bs-preview-layer {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: #000;
  display: flex;
  flex-direction: column;
}

.bs-preview-bar {
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #101a2c;
  color: #cfe3ff;
  font-size: 13px;
}

.bs-preview-body {
  flex: 1;
  min-height: 0;
}

.bs-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(7, 13, 24, 0.75);
  font-size: 14px;
  color: #cfe3ff;
}
</style>
