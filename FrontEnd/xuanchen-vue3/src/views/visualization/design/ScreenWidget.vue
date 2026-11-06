<template>
  <!-- 单个大屏组件渲染：设计器画布与展示页共用，保证「所见即所得」 -->
  <div class="bs-widget" :style="rootStyle">
    <!-- 文本 -->
    <div v-if="widget.type === 'text'" class="bs-text" :style="textStyle">
      {{ widget.props.text }}
    </div>

    <!-- 数值指标 -->
    <div v-else-if="widget.type === 'number'" class="bs-number">
      <div class="bs-number-title" :style="{ color: widget.props.titleColor || '#cfe3ff' }">
        {{ widget.props.title }}
      </div>
      <div class="bs-number-value" :style="numberStyle">
        {{ displayNumber }}
        <span v-if="widget.props.unit" class="bs-number-unit">{{ widget.props.unit }}</span>
      </div>
    </div>

    <!-- 图表：柱状 / 折线 / 饼 -->
    <div v-else-if="isChart" ref="chartRef" class="bs-chart"></div>

    <!-- 表格 -->
    <div v-else-if="widget.type === 'table'" class="bs-table">
      <div v-if="widget.props.title" class="bs-table-title">{{ widget.props.title }}</div>
      <div class="bs-table-body">
        <table>
          <thead>
            <tr>
              <th v-for="col in tableColumns" :key="col"
                :style="{ background: widget.props.headerBg || 'rgba(64,169,255,0.18)' }">
                {{ col }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in tableRows" :key="index"
              :class="{ 'is-striped': widget.props.striped !== false && index % 2 === 1 }">
              <td v-for="col in tableColumns" :key="col">{{ formatCell(row[col]) }}</td>
            </tr>
            <tr v-if="!tableRows.length">
              <td :colspan="tableColumns.length || 1" class="bs-table-empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 图片 -->
    <div v-else-if="widget.type === 'image'" class="bs-image">
      <img v-if="imageSrc" :src="imageSrc" :style="{ objectFit: widget.props.fit || 'cover' }" alt="" />
      <div v-else class="bs-image-empty">
        <PictureOutlined />
        <span>未设置图片</span>
      </div>
    </div>

    <!-- 边框容器 -->
    <div v-else-if="widget.type === 'border'" class="bs-border"
      :style="{ borderColor: widget.props.borderColor || 'rgba(64,169,255,0.45)' }">
      <div v-if="widget.props.title" class="bs-border-title"
        :style="{ color: widget.props.color || '#40a9ff', borderColor: widget.props.borderColor || 'rgba(64,169,255,0.45)' }">
        {{ widget.props.title }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { PictureOutlined } from '@ant-design/icons-vue';

import { buildChartOption, echarts, resolveNumberValue, resolveTableColumns } from './chartPresets';
import type { ScreenWidget } from '../visualization.types';

const props = withDefaults(defineProps<{
  widget: ScreenWidget;
  rows?: Record<string, unknown>[];
  columns?: string[];
}>(), {
  rows: () => [],
  columns: () => [],
});

const isChart = computed(() => ['bar', 'line', 'pie'].includes(props.widget.type));

const rootStyle = computed(() => ({
  background: props.widget.props?.background || 'transparent',
  borderRadius: `${props.widget.props?.radius ?? 0}px`,
}));

const textStyle = computed(() => {
  const conf = props.widget.props ?? {};
  return {
    fontSize: `${conf.fontSize ?? 24}px`,
    color: conf.color || '#ffffff',
    fontWeight: conf.fontWeight || 'normal',
    textAlign: conf.align || 'left',
    letterSpacing: `${conf.letterSpacing ?? 0}px`,
    lineHeight: 1.3,
  };
});

const numberStyle = computed(() => ({
  color: props.widget.props?.color || '#40a9ff',
  fontSize: `${props.widget.props?.fontSize ?? 40}px`,
}));

const displayNumber = computed(() => {
  const value = resolveNumberValue(props.widget, props.rows);
  const precision = Number(props.widget.props?.precision ?? 0);
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: precision,
    maximumFractionDigits: precision,
  });
});

const tableColumns = computed(() => resolveTableColumns(props.rows, props.columns));
const tableRows = computed(() => {
  const maxRows = Number(props.widget.props?.maxRows ?? 10);
  return props.rows.slice(0, maxRows);
});

const imageSrc = computed(() => String(props.widget.props?.src || ''));

const formatCell = (value: unknown): string => {
  if (value == null) return '';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
};

// ---------- 图表实例 ----------
const chartRef = ref<HTMLDivElement | null>(null);
let chart: any = null;
let observer: ResizeObserver | null = null;

const renderChart = () => {
  if (!isChart.value || !chartRef.value) return;
  if (!chart) {
    chart = echarts.init(chartRef.value);
  }
  chart.setOption(buildChartOption(props.widget, props.rows), true);
};

const initChart = () => {
  if (!chartRef.value) return;
  renderChart();
  if (!observer) {
    observer = new ResizeObserver(() => chart?.resize());
    observer.observe(chartRef.value);
  }
};

/** 销毁：切页/卸载必须 dispose，否则 canvas 与事件监听会残留 */
const disposeChart = () => {
  observer?.disconnect();
  observer = null;
  chart?.dispose();
  chart = null;
};

onMounted(() => {
  if (isChart.value) {
    nextTick(initChart);
  }
});

watch(
  () => [props.widget.type, props.widget.props, props.widget.data?.mapping, props.rows],
  () => {
    if (!isChart.value) {
      disposeChart();
      return;
    }
    nextTick(() => (chart ? renderChart() : initChart()));
  },
  { deep: true },
);

onBeforeUnmount(disposeChart);
</script>

<style scoped>
.bs-widget {
  width: 100%;
  height: 100%;
  overflow: hidden;
  box-sizing: border-box;
  color: #e6f0ff;
}

.bs-text {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  white-space: pre-wrap;
  word-break: break-all;
}

.bs-number {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  box-sizing: border-box;
  border: 1px solid rgba(64, 169, 255, 0.35);
}

.bs-number-title {
  font-size: 14px;
  letter-spacing: 1px;
  opacity: 0.85;
}

.bs-number-value {
  font-weight: 700;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.1;
}

.bs-number-unit {
  margin-left: 6px;
  font-size: 14px;
  font-weight: 400;
  opacity: 0.8;
}

.bs-chart {
  width: 100%;
  height: 100%;
}

.bs-table {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  box-sizing: border-box;
}

.bs-table-title {
  font-size: 15px;
  color: #e6f0ff;
  margin-bottom: 6px;
  padding-left: 2px;
}

.bs-table-body {
  flex: 1;
  overflow: hidden;
}

.bs-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.bs-table th,
.bs-table td {
  padding: 6px 8px;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 260px;
  border-bottom: 1px solid rgba(120, 170, 220, 0.12);
}

.bs-table th {
  color: #cfe3ff;
  font-weight: 500;
}

.bs-table tbody tr.is-striped {
  background: rgba(64, 169, 255, 0.06);
}

.bs-table-empty {
  text-align: center;
  padding: 16px 0;
  opacity: 0.5;
}

.bs-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13, 32, 64, 0.35);
}

.bs-image img {
  width: 100%;
  height: 100%;
  display: block;
}

.bs-image-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  opacity: 0.45;
}

.bs-border {
  width: 100%;
  height: 100%;
  border: 1px solid;
  position: relative;
  box-sizing: border-box;
}

.bs-border-title {
  position: absolute;
  top: -1px;
  left: -1px;
  padding: 4px 14px;
  font-size: 14px;
  border: 1px solid;
  border-left: none;
  border-top: none;
  background: rgba(13, 32, 64, 0.9);
}
</style>
