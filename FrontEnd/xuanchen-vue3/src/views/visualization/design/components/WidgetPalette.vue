<template>
  <!-- 左侧组件面板：点击添加到画布中央，或直接拖入画布指定位置 -->
  <div class="bs-palette">
    <div class="bs-panel-title">组件</div>
    <div v-for="group in groups" :key="group.name" class="bs-palette-group">
      <div class="bs-group-title">{{ group.name }}</div>
      <div class="bs-palette-list">
        <div v-for="item in group.items" :key="item.type" class="bs-palette-item" draggable="true"
          @dragstart="handleDragStart($event, item.type)" @click="emit('add', item.type)">
          <component :is="iconOf(item.icon)" class="bs-palette-icon" />
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  BarChartOutlined, BorderOutlined, DashboardOutlined, FontSizeOutlined,
  LineChartOutlined, PictureOutlined, PieChartOutlined, TableOutlined,
} from '@ant-design/icons-vue';

import { WIDGET_CATALOG } from '../widgetCatalog';
import type { WidgetType } from '../../visualization.types';

const emit = defineEmits<{
  (e: 'add', type: WidgetType): void;
}>();

const iconMap: Record<string, any> = {
  BarChartOutlined, LineChartOutlined, PieChartOutlined, TableOutlined,
  FontSizeOutlined, DashboardOutlined, PictureOutlined, BorderOutlined,
};

const iconOf = (name: string) => iconMap[name] ?? FontSizeOutlined;

const groups = computed(() => {
  const names = ['文本指标', '图表', '其他'] as const;
  return names.map((name) => ({
    name,
    items: WIDGET_CATALOG.filter((item) => item.group === name),
  }));
});

const handleDragStart = (event: DragEvent, type: WidgetType) => {
  event.dataTransfer?.setData('application/x-visualization-widget', type);
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy';
  }
};
</script>

<style scoped>
.bs-palette {
  width: 190px;
  flex-shrink: 0;
  height: 100%;
  overflow: auto;
  padding: 10px;
  box-sizing: border-box;
  background: #0d1729;
  border-right: 1px solid rgba(64, 169, 255, 0.18);
}

.bs-panel-title {
  font-size: 13px;
  color: #cfe3ff;
  opacity: 0.75;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.bs-palette-group {
  margin-bottom: 14px;
}

.bs-group-title {
  font-size: 12px;
  color: rgba(159, 196, 232, 0.7);
  margin-bottom: 6px;
}

.bs-palette-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.bs-palette-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 58px;
  font-size: 12px;
  color: #cfe3ff;
  background: rgba(64, 169, 255, 0.08);
  border: 1px solid rgba(64, 169, 255, 0.2);
  border-radius: 4px;
  cursor: grab;
  user-select: none;
  transition: all 0.15s;
}

.bs-palette-item:hover {
  background: rgba(64, 169, 255, 0.2);
  border-color: rgba(64, 169, 255, 0.55);
}

.bs-palette-icon {
  font-size: 18px;
}
</style>
