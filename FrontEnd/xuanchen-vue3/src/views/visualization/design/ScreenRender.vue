<template>
  <!-- 只读画布：按设计分辨率等比缩放铺到容器，设计器预览与展示页共用 -->
  <div ref="viewportRef" class="bs-render">
    <div class="bs-render-stage" :style="stageStyle">
      <div v-for="widget in sortedWidgets" :key="widget.id" class="bs-render-item"
        :style="itemStyle(widget)">
        <ScreenWidget :widget="widget" :rows="rowsOf(widget)" :columns="columnsOf(widget)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

import ScreenWidget from './ScreenWidget.vue';
import type { ScreenConfig, ScreenWidget as ScreenWidgetType } from '../visualization.types';

const props = withDefaults(defineProps<{
  config: ScreenConfig;
  /** 数据集取数结果：datasetKey -> rows/columns */
  datasetMap?: Record<string, { rows: Record<string, unknown>[]; columns: string[] }>;
}>(), {
  datasetMap: () => ({}),
});

const viewportRef = ref<HTMLDivElement | null>(null);
const scaleX = ref(1);
const scaleY = ref(1);

const sortedWidgets = computed<ScreenWidgetType[]>(() =>
  [...(props.config?.widgets ?? [])].sort((a, b) => (a.zIndex ?? 0) - (b.zIndex ?? 0)),
);

const computeScale = () => {
  const el = viewportRef.value;
  const canvas = props.config?.canvas;
  if (!el || !canvas) return;
  const { clientWidth, clientHeight } = el;
  if (!clientWidth || !clientHeight) return;
  scaleX.value = clientWidth / canvas.width;
  scaleY.value = clientHeight / canvas.height;
};

let observer: ResizeObserver | null = null;

onMounted(() => {
  computeScale();
  if (viewportRef.value) {
    observer = new ResizeObserver(computeScale);
    observer.observe(viewportRef.value);
  }
});

onBeforeUnmount(() => {
  observer?.disconnect();
  observer = null;
});

const stageStyle = computed(() => {
  const canvas = props.config.canvas;
  const full = canvas.scaleMode === 'full';
  const scale = full ? `${scaleX.value}, ${scaleY.value}` : `${Math.min(scaleX.value, scaleY.value)}`;
  return {
    width: `${canvas.width}px`,
    height: `${canvas.height}px`,
    backgroundColor: canvas.background || '#0b1020',
    backgroundImage: canvas.backgroundImage ? `url(${canvas.backgroundImage})` : 'none',
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    transform: `scale(${scale})`,
  };
});

const itemStyle = (widget: ScreenWidgetType) => ({
  left: `${widget.x}px`,
  top: `${widget.y}px`,
  width: `${widget.w}px`,
  height: `${widget.h}px`,
  zIndex: widget.zIndex,
});

const rowsOf = (widget: ScreenWidgetType): Record<string, unknown>[] => {
  if (widget.data?.mode === 'dataset' && widget.data.datasetKey) {
    return props.datasetMap[widget.data.datasetKey]?.rows ?? [];
  }
  return widget.data?.staticRows ?? [];
};

const columnsOf = (widget: ScreenWidgetType): string[] => {
  if (widget.data?.mode === 'dataset' && widget.data.datasetKey) {
    return props.datasetMap[widget.data.datasetKey]?.columns ?? [];
  }
  const rows = widget.data?.staticRows ?? [];
  return rows.length ? Object.keys(rows[0]) : [];
};
</script>

<style scoped>
.bs-render {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #050a16;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bs-render-stage {
  /* 布局盒子保持设计尺寸，用 scale 缩放：父容器 flex 居中 + transform-origin 居中
     可保证任意缩放比下画面都居中（改写 left/top + translate(-50%) 在缩放后会偏心） */
  flex: 0 0 auto;
  transform-origin: center center;
  overflow: hidden;
}

.bs-render-item {
  position: absolute;
}
</style>
