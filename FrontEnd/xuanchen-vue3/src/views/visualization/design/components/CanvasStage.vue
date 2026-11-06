<template>
  <!-- 设计画布：等比缩放显示设计分辨率，支持拖拽移动 / 四角缩放 / 选中 -->
  <div ref="viewportRef" class="bs-canvas-viewport" @dragover.prevent @drop="handleDrop" @pointerdown.self="emit('select', null)">
    <div ref="stageRef" class="bs-canvas-stage" :style="stageStyle" @pointerdown.self="emit('select', null)">
      <div v-if="config.canvas.showGrid" class="bs-canvas-grid"></div>

      <div v-for="widget in sortedWidgets" :key="widget.id" class="bs-canvas-item"
        :class="{ 'is-selected': widget.id === selectedId }" :style="itemStyle(widget)"
        @pointerdown="startDrag($event, widget, 'move')">
        <ScreenWidget :widget="widget" :rows="rowsOf(widget)" :columns="columnsOf(widget)" />

        <!-- 交互遮罩：图表内部 canvas 会吃掉指针事件，统一由遮罩承接拖拽 -->
        <div class="bs-canvas-mask"></div>

        <template v-if="widget.id === selectedId">
          <span class="bs-handle is-nw" @pointerdown.stop="startDrag($event, widget, 'resize-nw')"></span>
          <span class="bs-handle is-ne" @pointerdown.stop="startDrag($event, widget, 'resize-ne')"></span>
          <span class="bs-handle is-sw" @pointerdown.stop="startDrag($event, widget, 'resize-sw')"></span>
          <span class="bs-handle is-se" @pointerdown.stop="startDrag($event, widget, 'resize-se')"></span>
          <span class="bs-size-tip">{{ Math.round(widget.w) }} × {{ Math.round(widget.h) }}</span>
        </template>
      </div>

      <div v-if="!sortedWidgets.length" class="bs-canvas-empty">
        从左侧拖入组件，或直接点击左侧组件快速添加
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

import ScreenWidget from '../ScreenWidget.vue';
import type { ScreenConfig, ScreenWidget as ScreenWidgetType, WidgetType } from '../../visualization.types';

type DragMode = 'move' | 'resize-nw' | 'resize-ne' | 'resize-sw' | 'resize-se';

const props = withDefaults(defineProps<{
  config: ScreenConfig;
  selectedId?: string;
  datasetMap?: Record<string, { rows: Record<string, unknown>[]; columns: string[] }>;
}>(), {
  selectedId: '',
  datasetMap: () => ({}),
});

const emit = defineEmits<{
  (e: 'select', id: string | null): void;
  (e: 'add-widget', payload: { type: WidgetType; x: number; y: number }): void;
  (e: 'change'): void;
}>();

const viewportRef = ref<HTMLDivElement | null>(null);
const stageRef = ref<HTMLDivElement | null>(null);
const scale = ref(1);

const sortedWidgets = computed<ScreenWidgetType[]>(() =>
  [...(props.config?.widgets ?? [])].sort((a, b) => (a.zIndex ?? 0) - (b.zIndex ?? 0)),
);

const computeScale = () => {
  const el = viewportRef.value;
  const canvas = props.config?.canvas;
  if (!el || !canvas) return;
  const { clientWidth, clientHeight } = el;
  if (!clientWidth || !clientHeight) return;
  const padding = 24;
  scale.value = Math.min(
    (clientWidth - padding) / canvas.width,
    (clientHeight - padding) / canvas.height,
    1,
  );
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
  stopDrag();
});

const stageStyle = computed(() => ({
  width: `${props.config.canvas.width}px`,
  height: `${props.config.canvas.height}px`,
  backgroundColor: props.config.canvas.background || '#0b1020',
  backgroundImage: props.config.canvas.backgroundImage ? `url(${props.config.canvas.backgroundImage})` : 'none',
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  transform: `scale(${scale.value})`,
}));

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

// ---------- 拖拽 / 缩放 ----------
interface DragState {
  mode: DragMode;
  widget: ScreenWidgetType;
  startClientX: number;
  startClientY: number;
  originX: number;
  originY: number;
  originW: number;
  originH: number;
}

let dragState: DragState | null = null;

/** 网格吸附步长：显示网格时按 4px 对齐，避免组件边缘出现小数点 */
const snap = (value: number) => (props.config.canvas.showGrid ? Math.round(value / 4) * 4 : Math.round(value));

const startDrag = (event: PointerEvent, widget: ScreenWidgetType, mode: DragMode) => {
  if (event.button !== 0) return;
  event.stopPropagation();
  event.preventDefault();
  emit('select', widget.id);
  dragState = {
    mode,
    widget,
    startClientX: event.clientX,
    startClientY: event.clientY,
    originX: widget.x,
    originY: widget.y,
    originW: widget.w,
    originH: widget.h,
  };
  window.addEventListener('pointermove', onPointerMove);
  window.addEventListener('pointerup', stopDrag);
};

const MIN_SIZE = 40;

const onPointerMove = (event: PointerEvent) => {
  if (!dragState) return;
  const { mode, widget, startClientX, startClientY, originX, originY, originW, originH } = dragState;
  const dx = (event.clientX - startClientX) / (scale.value || 1);
  const dy = (event.clientY - startClientY) / (scale.value || 1);

  if (mode === 'move') {
    widget.x = Math.max(0, snap(originX + dx));
    widget.y = Math.max(0, snap(originY + dy));
  } else {
    // 缩放：保持对角落锚点不动，限制最小尺寸与左上边界
    if (mode === 'resize-se' || mode === 'resize-ne') {
      widget.w = Math.max(MIN_SIZE, snap(originW + dx));
    }
    if (mode === 'resize-sw' || mode === 'resize-nw') {
      const nextW = Math.max(MIN_SIZE, snap(originW - dx));
      widget.x = Math.max(0, originX + (originW - nextW));
      widget.w = nextW;
    }
    if (mode === 'resize-se' || mode === 'resize-sw') {
      widget.h = Math.max(MIN_SIZE, snap(originH + dy));
    }
    if (mode === 'resize-ne' || mode === 'resize-nw') {
      const nextH = Math.max(MIN_SIZE, snap(originH - dy));
      widget.y = Math.max(0, originY + (originH - nextH));
      widget.h = nextH;
    }
  }
  emit('change');
};

function stopDrag() {
  if (!dragState) return;
  dragState = null;
  window.removeEventListener('pointermove', onPointerMove);
  window.removeEventListener('pointerup', stopDrag);
}

/** 从左侧组件面板拖入：坐标换算到画布（stage）坐标系，再除以缩放比 */
const handleDrop = (event: DragEvent) => {
  const type = event.dataTransfer?.getData('application/x-visualization-widget') as WidgetType;
  if (!type || !stageRef.value) return;
  event.preventDefault();
  const rect = stageRef.value.getBoundingClientRect();
  const x = (event.clientX - rect.left) / (scale.value || 1);
  const y = (event.clientY - rect.top) / (scale.value || 1);
  emit('add-widget', { type, x: Math.max(0, snap(x)), y: Math.max(0, snap(y)) });
};
</script>

<style scoped>
.bs-canvas-viewport {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 20% 20%, rgba(64, 169, 255, 0.08), transparent 55%),
    #0a1120;
}

.bs-canvas-stage {
  position: relative;
  flex: 0 0 auto;
  transform-origin: center center;
  overflow: hidden;
  box-shadow: 0 0 0 1px rgba(64, 169, 255, 0.35), 0 12px 40px rgba(0, 0, 0, 0.45);
}

.bs-canvas-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(120, 170, 220, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(120, 170, 220, 0.08) 1px, transparent 1px);
  background-size: 40px 40px;
}

.bs-canvas-item {
  position: absolute;
  box-sizing: border-box;
  cursor: move;
}

.bs-canvas-item.is-selected {
  outline: 1px dashed #40a9ff;
  outline-offset: 0;
}

.bs-canvas-mask {
  position: absolute;
  inset: 0;
  z-index: 2;
}

.bs-handle {
  position: absolute;
  width: 10px;
  height: 10px;
  background: #40a9ff;
  border: 1px solid #ffffff;
  border-radius: 2px;
  z-index: 4;
}

.bs-handle.is-nw {
  left: -5px;
  top: -5px;
  cursor: nwse-resize;
}

.bs-handle.is-ne {
  right: -5px;
  top: -5px;
  cursor: nesw-resize;
}

.bs-handle.is-sw {
  left: -5px;
  bottom: -5px;
  cursor: nesw-resize;
}

.bs-handle.is-se {
  right: -5px;
  bottom: -5px;
  cursor: nwse-resize;
}

.bs-size-tip {
  position: absolute;
  right: 0;
  bottom: -22px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  color: #d6e9ff;
  background: rgba(20, 48, 90, 0.9);
  border-radius: 3px;
  z-index: 4;
  white-space: nowrap;
}

.bs-canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: rgba(207, 227, 255, 0.35);
  pointer-events: none;
}
</style>
