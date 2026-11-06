<template>
  <div class="designer-page">
    <!-- 顶部信息栏（hideHeader 时由外层全屏页的顶栏承载标题与操作按钮） -->
    <div v-if="!hideHeader" class="designer-header">
      <div class="designer-title">
        <span class="template-name">{{ template?.templateName || '未命名模板' }}</span>
        <a-tag :color="template?.status === 1 ? 'green' : 'default'" style="margin-left: 8px">
          {{ template?.status === 1 ? '启用' : '停用' }}
        </a-tag>
        <span v-if="template?.templateCode" class="template-code">{{ template.templateCode }}</span>
      </div>
      <div class="designer-actions">
        <a-button :loading="previewing" @click="handlePreview">
          <PlayCircleOutlined /> 试填充预览
        </a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <SaveOutlined /> 保存设计
        </a-button>
      </div>
    </div>

    <a-alert type="info" show-icon banner class="designer-tip"
      message="报表设计全部在这里完成：左侧管理数据集，画布设计版式（样式/合并/表头），选中单元格/区域后在右侧做字段绑定。报表新增时选择模板并复制本设计，之后与该模板解耦。" />

    <div class="designer-body">
      <!-- 左：数据集 -->
      <DatasetPanel ref="datasetPanelRef" class="panel-left" :template-id="templateId" :datasets="datasets"
        @datasets-change="onDatasetsChange" @data-executed="onDataExecuted" />

      <!-- 中：Univer 编辑画布 -->
      <div class="univer-wrap">
        <div id="template-designer-univer" class="univer-container"></div>
      </div>

      <!-- 右：单元格绑定 -->
      <BindingPanel class="panel-right" :selection="currentSelection" :bindings="bindings" :datasets="datasets"
        :dataset-columns="datasetColumns" @bindings-change="onBindingsChange" />
    </div>

    <!-- 试填充预览弹窗 -->
    <a-modal v-model:open="previewVisible" title="试填充预览" :width="1000" destroy-on-close :footer="null"
      @after-close="disposePreview">
      <a-alert v-if="previewWarnings.length" type="warning" show-icon style="margin-bottom: 8px">
        <template #message>
          <div v-for="(w, i) in previewWarnings" :key="i">{{ w }}</div>
        </template>
      </a-alert>
      <div id="report-preview-univer" class="preview-container"></div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';

import { getTemplateConfigApi, saveTemplateConfigApi } from './template.api';
import { executeDatasetApi } from '../report.api';
import type { TemplateRecord } from './template.types';
import type { DatasetRecord, ReportConfig, ReportBinding, DatasetResult } from '../report.types';
import { blankSnapshot, createSheetsInstance, createWorkbookUnit } from '../design/univerSetup';
import type { UniverInstance } from '../design/univerSetup';
import { fillSnapshot } from '../viewer/fillEngine';
import type { DatasetDataMap } from '../viewer/fillEngine';
import DatasetPanel from '../design/components/DatasetPanel.vue';
import BindingPanel from '../design/components/BindingPanel.vue';

/**
 * 模板设计器：报表的全部设计内容都在这里完成（版式 + 数据集 + 字段绑定 + 试填充预览）。
 * 保存写 rpt_template.config_json = { snapshot, params, bindings }；
 * 报表新增时复制该 JSON，之后与模板解耦（改模板不影响已建报表）。
 *
 * 使用方式：由「模板设计」列表的「设计」以弹窗形式打开（template/modal/DesignerModal.vue），
 * 组件本身不依赖路由，模板ID由父组件传入；关闭时通过 close 事件交回父组件。
 */
const props = defineProps<{
  /** 所属模板ID */
  templateId: string;
  /** 隐藏组件自带的标题/操作栏（全屏页模式：操作按钮由外层顶栏承载） */
  hideHeader?: boolean;
}>();

const emit = defineEmits<{
  /** 请求关闭（未保存时已做过确认） */
  (e: 'close'): void;
  /** 保存成功（父级可据此刷新列表） */
  (e: 'saved'): void;
}>();

const template = ref<TemplateRecord | null>(null);
const datasets = ref<DatasetRecord[]>([]);
const config = reactive<ReportConfig>({ snapshot: blankSnapshot(), params: [], bindings: {} });
const bindings = ref<Record<string, ReportBinding>>({});
const datasetColumns = ref<Record<string, string[]>>({});

const datasetPanelRef = ref();
const saving = ref(false);
const previewing = ref(false);

let univerInstance: UniverInstance | null = null;
let disposeSelection: { dispose: () => void } | null = null;

/** 当前选区（来自 Univer onSelectionChange） */
const currentSelection = ref<{ refText: string; rangeText: string; sheetId: string } | null>(null);

// ---------- 数据集 / 绑定 ----------

const onDatasetsChange = (list: DatasetRecord[]) => {
  datasets.value = list;
};

/** 数据集试执行完成后记录字段列表（绑定面板下拉用） */
const onDataExecuted = (payload: { datasetCode: string; result: DatasetResult }) => {
  datasetColumns.value = {
    ...datasetColumns.value,
    [payload.datasetCode]: payload.result.columns ?? [],
  };
};

const onBindingsChange = (next: Record<string, ReportBinding>) => {
  bindings.value = next;
};

/** 选区 IRange -> 'A1' / 'A1:B2' 文本 */
const toSelectionText = (selections: any[]) => {
  if (!selections || !selections.length) return;
  const range = selections[0];
  const startRow = range.startRow ?? 0;
  const startColumn = range.startColumn ?? 0;
  const endRow = range.endRow ?? startRow;
  const endCol = range.endColumn ?? startColumn;
  const colName = (i: number) => {
    let n = i + 1;
    let name = '';
    while (n > 0) {
      const mod = (n - 1) % 26;
      name = String.fromCharCode(65 + mod) + name;
      n = Math.floor((n - 1) / 26);
    }
    return name;
  };
  const refText = `${colName(startColumn)}${startRow + 1}`;
  const singleRow = startRow === endRow;
  const rangeText = singleRow && startColumn === endCol
    ? refText
    : `${colName(startColumn)}${startRow + 1}:${colName(endCol)}${endRow + 1}`;
  currentSelection.value = {
    refText,
    rangeText,
    sheetId: (univerInstance?.univerAPI.getActiveWorkbook()?.getActiveSheet()?.getSheetId() as string) ?? 'sheet1',
  };
};

// ---------- 保存 ----------

/** 取画布当前快照（编辑态的实时内容） */
const captureSnapshot = () => {
  const workbook = univerInstance?.univerAPI.getActiveWorkbook();
  if (!workbook) return null;
  try {
    return workbook.save() as unknown as Record<string, unknown>;
  } catch {
    return null;
  }
};

const handleSave = async () => {
  const snapshot = captureSnapshot();
  if (!snapshot) {
    message.error('设计画布未就绪');
    return;
  }
  saving.value = true;
  try {
    const payload: ReportConfig = {
      snapshot,
      params: config.params ?? [],
      bindings: bindings.value,
    };
    const res = await saveTemplateConfigApi(props.templateId, JSON.stringify(payload));
    if (res.code !== 200) {
      message.error(res.msg || '保存失败');
      return;
    }
    config.snapshot = snapshot;
    config.bindings = bindings.value;
    baselineSnapshotJson = JSON.stringify(snapshot);
    baselineBindingsJson = JSON.stringify(bindings.value);
    message.success('模板设计已保存！');
    emit('saved');
  } finally {
    saving.value = false;
  }
};

// ---------- 试填充预览 ----------

const previewVisible = ref(false);
const previewWarnings = ref<string[]>([]);
let previewInstance: UniverInstance | null = null;

const handlePreview = async () => {
  const snapshot = captureSnapshot();
  if (!snapshot) {
    message.error('设计画布未就绪');
    return;
  }
  previewing.value = true;
  try {
    // 依次试执行所有数据集
    const dataMap: DatasetDataMap = {};
    for (const ds of datasets.value) {
      if (!ds.id || !ds.datasetCode) continue;
      const res = await executeDatasetApi(ds.id, {});
      if (res.code !== 200 || !res.data) {
        message.warning(`数据集 ${ds.datasetCode} 执行失败：${res.msg ?? '未知错误'}`);
        continue;
      }
      dataMap[ds.datasetCode] = res.data;
    }
    const outcome = fillSnapshot(
      { snapshot, params: config.params ?? [], bindings: bindings.value },
      dataMap
    );
    previewWarnings.value = outcome.warnings;
    previewVisible.value = true;
    // 等弹窗 DOM 渲染完成后挂载只读 Univer
    setTimeout(() => {
      previewInstance?.dispose();
      previewInstance = createSheetsInstance({
        container: 'report-preview-univer',
        editable: false,
        header: false,
        toolbar: false,
        formulaBar: false,
        footer: false,
        contextMenu: false,
      });
      createWorkbookUnit(previewInstance.univerAPI, outcome.snapshot as Record<string, unknown>, false);
    }, 60);
  } finally {
    previewing.value = false;
  }
};

const disposePreview = () => {
  previewInstance?.dispose();
  previewInstance = null;
};

// ---------- 未保存改动判定（供外层弹窗关闭前拦截） ----------

let baselineSnapshotJson = '';
let baselineBindingsJson = '{}';

const hasUnsavedChanges = () => {
  const snapshot = captureSnapshot();
  if (snapshot && JSON.stringify(snapshot) !== baselineSnapshotJson) return true;
  return JSON.stringify(bindings.value) !== baselineBindingsJson;
};

defineExpose({
  hasUnsavedChanges,
  /** 保存设计（外层顶栏按钮调用） */
  save: handleSave,
  /** 试填充预览（外层顶栏按钮调用） */
  preview: handlePreview,
  /** 模板信息（外层顶栏展示标题/状态用） */
  template,
  saving,
  previewing,
});

// ---------- 初始化 ----------

const applyConfigJson = (configJson?: string) => {
  try {
    const parsed = JSON.parse(configJson ?? '{}') as ReportConfig;
    config.snapshot = parsed.snapshot ?? blankSnapshot();
    config.params = parsed.params ?? [];
    config.bindings = parsed.bindings ?? {};
    bindings.value = config.bindings;
  } catch {
    config.snapshot = blankSnapshot();
    message.warning('原配置解析失败，已加载空白模板');
  }
};

onMounted(async () => {
  if (!props.templateId) {
    message.error('缺少模板ID');
    emit('close');
    return;
  }
  try {
    const data = await getTemplateConfigApi(props.templateId);
    template.value = data.template;
    applyConfigJson(data.template.configJson);
  } catch (error) {
    message.error(typeof error === 'string' ? error : '加载模板失败');
    emit('close');
    return;
  }

  univerInstance = createSheetsInstance({ container: 'template-designer-univer' });
  const workbook = createWorkbookUnit(univerInstance.univerAPI, config.snapshot as Record<string, unknown>, true);
  disposeSelection = workbook.onSelectionChange((selections: any[]) => toSelectionText(selections)) as unknown as {
    dispose: () => void;
  };
  // 以「刚挂载时的画布内容」为脏检查基线，避免把 Univer 自动补全的默认字段误判为改动
  baselineSnapshotJson = JSON.stringify(captureSnapshot() ?? {});
  baselineBindingsJson = JSON.stringify(bindings.value);
});

onBeforeUnmount(() => {
  disposeSelection?.dispose();
  previewInstance?.dispose();
  univerInstance?.dispose();
  univerInstance = null;
});
</script>

<style scoped>
.designer-page {
  display: flex;
  flex-direction: column;
  /* 由外层弹窗（DesignerModal）控制高度，组件自身铺满容器 */
  height: 100%;
  min-height: 420px;
}

.designer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(128, 128, 128, 0.2);
}

.designer-title {
  display: flex;
  align-items: center;
}

.template-name {
  font-size: 15px;
  font-weight: 500;
}

.template-code {
  margin-left: 10px;
  font-size: 12px;
  opacity: 0.6;
}

.designer-actions {
  display: flex;
  gap: 8px;
}

.designer-tip {
  margin: 0;
  border-radius: 0;
}

.designer-body {
  display: flex;
  flex: 1;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.panel-left {
  width: 300px;
  flex-shrink: 0;
  overflow: auto;
}

.panel-right {
  width: 320px;
  flex-shrink: 0;
  overflow: auto;
}

.univer-wrap {
  flex: 1;
  min-width: 0;
}

.univer-container {
  height: 100%;
  width: 100%;
}

.preview-container {
  height: 560px;
  width: 100%;
}
</style>
