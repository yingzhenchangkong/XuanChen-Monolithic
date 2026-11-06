<template>
  <a-card size="small" title="单元格绑定" class="binding-panel">
    <a-alert v-if="!selection" type="info" show-icon message="先在表格中选中单元格/区域，再添加绑定" />
    <template v-else>
      <div class="selection-box">
        当前选区：<a-tag color="blue">{{ selection.rangeText }}</a-tag>
      </div>
      <div class="add-actions">
        <a-button size="small" :disabled="!hasDataset" @click="addListBinding">
          <OrderedListOutlined /> 绑定明细区
        </a-button>
        <a-button size="small" :disabled="!hasDataset" @click="addCellBinding">
          <FocusOutlined /> 绑定单值
        </a-button>
      </div>
    </template>

    <a-divider style="margin: 8px 0"></a-divider>
    <div class="binding-title">已绑定（{{ bindingList.length }}）</div>
    <a-empty v-if="!bindingList.length" description="暂无绑定" :image-style="{ height: '40px' }" />

    <div v-for="(item, index) in bindingList" :key="item.key" class="binding-item">
      <div class="binding-head">
        <a-tag :color="item.binding.type === 'list' ? 'green' : 'purple'">
          {{ item.binding.type === 'list' ? '明细' : '单值' }}
        </a-tag>
        <span class="binding-ref">{{ item.binding.type === 'list' ? item.binding.range : item.binding.ref }}</span>
        <a class="binding-remove" @click="removeBinding(item.key)">
          <DeleteOutlined />
        </a>
      </div>
      <div class="binding-body">
        <a-select v-model:value="item.binding.dataset" size="small" style="width: 100%; margin-bottom: 6px"
          :options="datasetOptions" placeholder="选择数据集" @change="() => onDatasetChange(item)" />
        <!-- 明细区：逐列映射字段 -->
        <template v-if="item.binding.type === 'list'">
          <div v-for="col in listBindingColumns(item.binding)" :key="col" class="map-row">
            <span class="map-col">{{ col }}</span>
            <a-select v-model:value="columnMapOf(item.binding)[col]" size="small" style="flex: 1" allow-clear
              :options="fieldOptions(item.binding.dataset)" placeholder="字段"
              @change="() => emitChange()" />
          </div>
        </template>
        <!-- 单值：字段 + 聚合 -->
        <template v-else>
          <div class="map-row">
            <a-select v-model:value="item.binding.field" size="small" style="flex: 1"
              :options="fieldOptions(item.binding.dataset)" placeholder="字段"
              @change="() => emitChange()" />
            <a-select v-model:value="item.binding.agg" size="small" style="width: 90px" allow-clear
              :options="aggOptions" placeholder="聚合" @change="() => emitChange()" />
          </div>
        </template>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { message } from 'ant-design-vue';
import { DeleteOutlined, OrderedListOutlined, AimOutlined as FocusOutlined } from '@ant-design/icons-vue';

import type { DatasetRecord, ReportBinding, ReportListBinding, ReportCellBinding } from '../../report.types';

const props = defineProps<{
  selection: { refText: string; rangeText: string; sheetId: string } | null;
  bindings: Record<string, ReportBinding>;
  datasets: DatasetRecord[];
  datasetColumns: Record<string, string[]>;
}>();

const emit = defineEmits<{
  (e: 'bindingsChange', next: Record<string, ReportBinding>): void;
}>();

const aggOptions = [
  { label: 'SUM', value: 'SUM' },
  { label: 'AVG', value: 'AVG' },
  { label: 'COUNT', value: 'COUNT' },
  { label: 'MAX', value: 'MAX' },
  { label: 'MIN', value: 'MIN' },
];

const hasDataset = computed(() => props.datasets.length > 0);
const datasetOptions = computed(() =>
  props.datasets.map((d) => ({ label: `${d.datasetName}（${d.datasetCode}）`, value: String(d.datasetCode) }))
);

const bindingList = computed(() =>
  Object.entries(props.bindings).map(([key, binding]) => ({ key, binding }))
);

const emitChange = () => {
  emit('bindingsChange', { ...props.bindings });
};

const fieldOptions = (datasetCode?: string) =>
  (props.datasetColumns[datasetCode ?? ''] ?? []).map((f) => ({ label: f, value: f }));

const listBindingColumns = (binding: ReportListBinding) => {
  if (!binding.range) return [];
  const [, to] = binding.range.split(':');
  const from = binding.range.split(':')[0];
  const colLetter = (refText: string) => /^[A-Z]+/i.exec(refText)?.[0].toUpperCase() ?? 'A';
  const colIndex = (name: string) => {
    let index = 0;
    for (const ch of name) index = index * 26 + (ch.charCodeAt(0) - 64);
    return index - 1;
  };
  const start = colIndex(colLetter(from));
  const end = to ? colIndex(colLetter(to)) : start;
  const cols: string[] = [];
  for (let i = start; i <= end; i++) {
    let n = i + 1;
    let name = '';
    while (n > 0) {
      const mod = (n - 1) % 26;
      name = String.fromCharCode(65 + mod) + name;
      n = Math.floor((n - 1) / 26);
    }
    cols.push(name);
  }
  return cols;
};

const columnMapOf = (binding: ReportListBinding) => {
  if (!binding.columns) binding.columns = {};
  return binding.columns;
};

const onDatasetChange = (item: { binding: ReportBinding }) => {
  // 切换数据集后清空列映射的字段，避免串数据
  if (item.binding.type === 'list') {
    const columns = item.binding.columns ?? {};
    for (const col of Object.keys(columns)) {
      columns[col] = typeof columns[col] === 'string' ? '' : { ...columns[col], field: '' };
    }
  }
  emitChange();
};

const addListBinding = () => {
  const selection = props.selection!;
  if (!selection.rangeText.includes(':')) {
    message.warning('明细区请选中一行内的连续列区域（如 A5:F5）');
    return;
  }
  const [from, to] = selection.rangeText.split(':');
  const rowFrom = Number(/\d+/.exec(from)?.[0] ?? 1);
  const rowTo = Number(/\d+/.exec(to)?.[0] ?? rowFrom);
  if (rowFrom !== rowTo) {
    message.warning('明细区模板行只支持单行区域，请调整选区');
    return;
  }
  const key = `list:${selection.rangeText}`;
  const binding: ReportListBinding = {
    type: 'list',
    anchor: selection.refText,
    range: selection.rangeText,
    dataset: String(props.datasets[0]?.datasetCode ?? ''),
    columns: {},
  };
  props.bindings[key] = binding;
  emitChange();
};

const addCellBinding = () => {
  const selection = props.selection!;
  const key = `cell:${selection.refText}`;
  const binding: ReportCellBinding = {
    type: 'cell',
    ref: selection.refText,
    dataset: String(props.datasets[0]?.datasetCode ?? ''),
    field: '',
    agg: '',
  };
  props.bindings[key] = binding;
  emitChange();
};

const removeBinding = (key: string) => {
  delete props.bindings[key];
  emitChange();
};
</script>

<style scoped>
.selection-box {
  margin-bottom: 8px;
  font-size: 13px;
}

.add-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.binding-title {
  font-weight: 500;
  margin: 4px 0;
}

.binding-item {
  padding: 6px 0;
  border-bottom: 1px dashed rgba(128, 128, 128, 0.25);
}

.binding-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.binding-ref {
  flex: 1;
  font-size: 12px;
  font-family: monospace;
}

.binding-body {
  margin-top: 4px;
}

.map-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.map-col {
  width: 22px;
  font-family: monospace;
  font-size: 12px;
  text-align: center;
  border: 1px solid rgba(128, 128, 128, 0.3);
  border-radius: 4px;
}
</style>
