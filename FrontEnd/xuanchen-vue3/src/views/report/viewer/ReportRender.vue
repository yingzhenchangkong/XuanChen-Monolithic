<template>
  <div class="report-render">
    <!-- 顶栏：参数 + 查询 + 操作 -->
    <div class="render-header">
      <a-form v-if="paramList.length" layout="inline" class="param-form">
        <a-form-item v-for="p in paramList" :key="p.code" :label="p.label">
          <a-date-picker v-if="p.type === 'date'" v-model:value="paramValues[p.code]" value-format="YYYY-MM-DD"
            style="width: 150px" placeholder="选择日期" />
          <a-select v-else-if="p.type === 'dict'" v-model:value="paramValues[p.code]" style="width: 150px"
            :options="dictOptions[p.dictType ?? ''] ?? []" placeholder="请选择" allow-clear />
          <a-input-number v-else-if="p.type === 'number'" v-model:value="paramValues[p.code]" style="width: 150px"
            placeholder="请输入" />
          <a-input v-else v-model:value="paramValues[p.code]" style="width: 150px" placeholder="请输入" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="loading" @click="loadAndRender">
            <SearchOutlined /> 查询
          </a-button>
        </a-form-item>
      </a-form>
      <div class="render-actions">
        <a-button :disabled="!rendered" :loading="loading" @click="loadAndRender">
          <ReloadOutlined /> 刷新
        </a-button>
        <a-tooltip v-if="paramList.length" title="重置参数为默认值并重新查询">
          <a-button :disabled="loading" @click="resetParams"><ClearOutlined /></a-button>
        </a-tooltip>
        <a-dropdown>
          <a-button :disabled="!rendered">
            <DownloadOutlined /> 导出 <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu @click="onExport">
              <a-menu-item key="xlsx">导出 Excel (.xlsx)</a-menu-item>
              <a-menu-item key="pdf">导出 PDF (.pdf)</a-menu-item>
              <a-menu-item key="docx">导出 Word (.docx)</a-menu-item>
              <a-menu-item key="csv">导出 CSV</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button type="primary" ghost :disabled="!rendered" @click="onPrint">
          <PrinterOutlined /> 打印
        </a-button>
        <a-tooltip :title="isFullscreen ? '退出全屏' : '全屏'">
          <a-button @click="toggleFullscreen">
            <FullscreenExitOutlined v-if="isFullscreen" />
            <FullscreenOutlined v-else />
          </a-button>
        </a-tooltip>
      </div>
    </div>

    <a-alert v-if="warnings.length" type="warning" show-icon class="render-warning">
      <template #message>
        <div v-for="(w, i) in warnings" :key="i">{{ w }}</div>
      </template>
    </a-alert>

    <!-- 只读渲染画布 -->
    <div ref="bodyRef" class="render-body">
      <div ref="containerRef" class="render-container"></div>
      <a-empty v-if="!rendered" class="render-empty" :description="emptyText" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import {
  ClearOutlined,
  DownloadOutlined,
  DownOutlined,
  FullscreenExitOutlined,
  FullscreenOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue';

import { getViewConfigApi, fetchDatasetDataApi } from '../report.api';
import type { ReportConfig, ViewConfigData, DatasetParam, DatasetResult } from '../report.types';
import { createSheetsInstance, createWorkbookUnit } from '../design/univerSetup';
import type { UniverInstance } from '../design/univerSetup';
import { fillSnapshot, type Snapshot } from '../viewer/fillEngine';
import type { DatasetDataMap } from '../viewer/fillEngine';
import { exportCsv, exportDocx, exportPdf, exportXlsx, printSnapshot } from '../viewer/exportEngine';

const props = defineProps<{
  /** 报表编码（查看页路由用，全库唯一） */
  reportCode: string;
}>();

const containerRef = ref<HTMLDivElement | null>(null);
const bodyRef = ref<HTMLDivElement | null>(null);

const config = ref<ViewConfigData | null>(null);
const parsedConfig = ref<ReportConfig>({});
const paramList = ref<DatasetParam[]>([]);
const paramValues = reactive<Record<string, string | number | null>>({});
const dictOptions = reactive<Record<string, { label: string; value: string }[]>>({});

const loading = ref(false);
const rendered = ref(false);
const warnings = ref<string[]>([]);
const emptyText = ref('点击"查询"加载报表数据');
const isFullscreen = ref(false);

/** 最近一次填充后的快照：导出/打印的数据源（与渲染同源） */
let lastFilled: Snapshot | null = null;

let univerInstance: UniverInstance | null = null;

/** 渲染用参数定义：优先报表级 params，否则合并各数据集的 paramsJson */
const buildParams = (cfg: ReportConfig, view: ViewConfigData) => {
  if (cfg.params && cfg.params.length) {
    paramList.value = cfg.params;
  } else {
    const merged: DatasetParam[] = [];
    const seen = new Set<string>();
    for (const ds of view.datasets) {
      if (!ds.paramsJson) continue;
      try {
        const list = JSON.parse(ds.paramsJson) as DatasetParam[];
        for (const p of list) {
          if (!seen.has(p.code)) {
            seen.add(p.code);
            merged.push(p);
          }
        }
      } catch {
        // 忽略非法参数定义
      }
    }
    paramList.value = merged;
  }
  for (const p of paramList.value) {
    paramValues[p.code] = (p.defaultValue as string | number | null) ?? null;
  }
};

/** 需要取数的数据集：绑定关系里出现过的 */
const usedDatasets = computed(() => {
  const codes = new Set<string>();
  for (const binding of Object.values(parsedConfig.value.bindings ?? {})) {
    if (binding.dataset) codes.add(binding.dataset);
  }
  return codes;
});

const loadAndRender = async () => {
  if (!config.value) return;
  loading.value = true;
  warnings.value = [];
  try {
    // 必填参数校验
    for (const p of paramList.value) {
      if (p.required && (paramValues[p.code] === null || paramValues[p.code] === undefined || paramValues[p.code] === '')) {
        message.warning(`请填写 ${p.label}`);
        return;
      }
    }
    // 逐数据集取数
    const dataMap: DatasetDataMap = {};
    for (const code of usedDatasets.value) {
      const res = await fetchDatasetDataApi(props.reportCode, code, { ...paramValues });
      if (res.code !== 200 || !res.data) {
        warnings.value.push(`数据集 ${code} 取数失败：${res.msg ?? '未知错误'}`);
        continue;
      }
      dataMap[code] = res.data;
    }
    const outcome = fillSnapshot(parsedConfig.value, dataMap);
    warnings.value.push(...outcome.warnings);
    lastFilled = outcome.snapshot as Snapshot;
    renderSnapshot(outcome.snapshot as Record<string, unknown>);
    emptyText.value = '该报表没有可渲染的配置';
  } finally {
    loading.value = false;
  }
};

const renderSnapshot = (snapshot: Record<string, unknown>) => {
  univerInstance?.dispose();
  univerInstance = createSheetsInstance({
    container: containerRef.value!,
    editable: false,
    header: false,
    toolbar: false,
    formulaBar: false,
    footer: false,
    contextMenu: false,
  });
  createWorkbookUnit(univerInstance.univerAPI, snapshot, false);
  rendered.value = true;
};

onMounted(async () => {
  document.addEventListener('fullscreenchange', onFullscreenChange);
  if (!props.reportCode) {
    emptyText.value = '缺少报表编码';
    return;
  }
  try {
    const view = await getViewConfigApi(props.reportCode);
    config.value = view;
    const parsed = JSON.parse(view.configJson ?? '{}') as ReportConfig;
    parsedConfig.value = {
      snapshot: parsed.snapshot ?? {},
      params: parsed.params ?? [],
      bindings: parsed.bindings ?? {},
    };
    buildParams(parsedConfig.value, view);
    // 无参数的报表自动查询一次
    if (paramList.value.length === 0) {
      await loadAndRender();
    }
  } catch (e) {
    emptyText.value = String(e instanceof Error ? e.message : e);
  }
});

onBeforeUnmount(() => {
  univerInstance?.dispose();
  univerInstance = null;
  document.removeEventListener('fullscreenchange', onFullscreenChange);
});

/** 重置参数为默认值并重新查询 */
const resetParams = async () => {
  for (const p of paramList.value) {
    paramValues[p.code] = (p.defaultValue as string | number | null) ?? null;
  }
  await loadAndRender();
};

/** 导出：xlsx / pdf / docx / csv */
const onExport = async ({ key }: { key: string | number }) => {
  if (!lastFilled) return;
  const fileName = config.value?.reportName || props.reportCode;
  try {
    switch (key) {
      case 'xlsx':
        await exportXlsx(lastFilled, fileName);
        message.success('已导出 Excel');
        break;
      case 'pdf':
        message.loading({ content: '正在生成 PDF…', key: 'export-pdf', duration: 0 });
        await exportPdf(lastFilled, fileName);
        message.success({ content: '已导出 PDF', key: 'export-pdf' });
        break;
      case 'docx':
        await exportDocx(lastFilled, fileName);
        message.success('已导出 Word');
        break;
      default:
        exportCsv(lastFilled, fileName);
        message.success('已导出 CSV');
    }
  } catch (e) {
    message.error({ content: `导出失败：${e instanceof Error ? e.message : String(e)}`, key: 'export-pdf' });
  }
};

/** 打印（浏览器打印对话框中可另存为 PDF） */
const onPrint = () => {
  if (!lastFilled) return;
  printSnapshot(lastFilled, config.value?.reportName || props.reportCode);
};

/** 全屏切换 */
const onFullscreenChange = () => {
  isFullscreen.value = document.fullscreenElement === bodyRef.value;
};
const toggleFullscreen = () => {
  if (document.fullscreenElement === bodyRef.value) {
    document.exitFullscreen();
  } else {
    bodyRef.value?.requestFullscreen();
  }
};
</script>

<style scoped>
.report-render {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 480px;
}

.render-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(128, 128, 128, 0.2);
}

.render-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.render-body:fullscreen {
  background: var(--color-background-primary, #fff);
  padding: 12px;
}

.param-form {
  row-gap: 4px;
}

.render-warning {
  margin: 8px 12px 0;
}

.render-body {
  position: relative;
  flex: 1;
  padding: 8px;
  overflow: hidden;
}

.render-container {
  height: 100%;
  width: 100%;
}

.render-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
</style>
