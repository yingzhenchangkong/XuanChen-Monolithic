<template>
  <div class="api-editor">
    <!-- 顶部工具条 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <a-button size="small" @click="handleBack">
          <template #icon>
            <ArrowLeftOutlined />
          </template>返回
        </a-button>
        <a-input v-model:value="apiName" size="small" class="doc-name-input" :maxlength="100" />
        <a-tag color="blue">/open/api/{{ apiCode }}</a-tag>
        <a-tag :color="statusColor">{{ statusText }}</a-tag>
      </div>
      <div class="toolbar-right">
        <a-space :size="4">
          <a-button size="small" :loading="testing" @click="runTest">测试运行</a-button>
          <a-button v-if="status !== 1" size="small" @click="handlePublish">
            <template #icon>
              <CheckCircleOutlined />
            </template>发布
          </a-button>
          <a-button v-else size="small" danger @click="handleStop">停用</a-button>
          <a-button size="small" type="primary" :loading="saving" @click="handleSave">
            <template #icon>
              <SaveOutlined />
            </template>保存
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="body">
      <!-- 左：基本信息 + 步骤链 -->
      <div class="editor-pane">
        <a-card title="基本信息" size="small" class="block">
          <a-form layout="vertical">
            <a-row :gutter="12">
              <a-col :span="8">
                <a-form-item label="接口编码" required help="发布后访问 /open/api/{接口编码}">
                  <a-input v-model:value="apiCode" :disabled="!!apiId" placeholder="字母开头，字母数字-_" :maxlength="64" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="访问密钥" help="留空=内部免鉴权；非空则调用须带 X-Api-Key 头">
                  <a-input v-model:value="apiKey" placeholder="留空免鉴权" :maxlength="64" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="备注">
                  <a-input v-model:value="remark" placeholder="接口用途说明" :maxlength="255" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>

        <a-card size="small" class="block">
          <template #title>
            步骤链
            <span class="step-hint">自上而下执行，后续步骤可用 {{ exprHint }} 引用前面输出</span>
          </template>
          <template #extra>
            <a-dropdown>
              <a-button size="small" type="primary">
                <template #icon>
                  <PlusOutlined />
                </template>添加步骤
              </a-button>
              <template #overlay>
                <a-menu @click="addStep">
                  <a-menu-item key="sql">SQL 查询（自己的数据库）</a-menu-item>
                  <a-menu-item key="http">HTTP 拉取（别人的 API）</a-menu-item>
                  <a-menu-item key="map">字段映射</a-menu-item>
                  <a-menu-item key="save">采集落库</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>

          <a-empty v-if="steps.length === 0" description="尚未配置步骤，请点击右上角「添加步骤」" />
          <div v-for="(step, idx) in steps" :key="idx" class="step-card">
            <div class="step-head">
              <a-tag :color="stepColor(step.type)" class="step-type">{{ stepLabel(step.type) }}</a-tag>
              <a-input v-model:value="step.name" size="small" class="step-name" placeholder="步骤名（后续引用用）" :maxlength="32" />
              <a-button size="small" :disabled="idx === 0" @click="moveStep(idx, -1)">上移</a-button>
              <a-button size="small" :disabled="idx === steps.length - 1" @click="moveStep(idx, 1)">下移</a-button>
              <a-button size="small" danger @click="removeStep(idx)">删除</a-button>
            </div>

            <!-- 按类型渲染配置表单（子组件内直接编辑草稿对象） -->
            <ApiStepForm :step="step" :datasource-list="datasourceList" />
          </div>
        </a-card>
      </div>

      <!-- 右：测试运行 -->
      <div class="test-pane">
        <a-card title="测试运行" size="small" class="block">
          <a-form-item :label="`入参（JSON 对象，对应 ${paramsHint}）`">
            <a-textarea v-model:value="testParams" :rows="3" placeholder='{ "status": 1 }' />
          </a-form-item>
          <a-button type="primary" block :loading="testing" @click="runTest">执行测试</a-button>
          <a-alert v-if="testError" class="test-result" type="error" :message="testError" show-icon />
          <template v-if="testResult !== null">
            <div class="test-meta">耗时 {{ testCostMs }} ms</div>
            <pre class="test-pre">{{ testResult }}</pre>
          </template>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined, CheckCircleOutlined, PlusOutlined, SaveOutlined } from '@ant-design/icons-vue';

import {
  getApiDetailApi,
  publishApiApi,
  saveApiApi,
  stopApiApi,
  testApiApi,
  getDatasourceListApi,
} from '../designer.api';
import type { ApiDatasourceRecord, ApiRecord, StepDefItem } from '../designer.api';
import ApiStepForm from './ApiStepForm.vue';

interface StepDraft {
  type: StepDefItem['type'];
  name: string;
  //sql
  datasource: string;
  sqlText: string;
  paramText: string;
  //http
  url: string;
  method: string;
  headerText: string;
  bodyText: string;
  //map
  mapSource: string;
  keepSource: boolean;
  fieldsText: string;
  //save
  saveSource: string;
}

const route = useRoute();
const router = useRouter();

const apiId = route.params.id as string;
const apiCode = ref('');
const apiName = ref('');
const apiKey = ref('');
const remark = ref('');
const status = ref(0);
const saving = ref(false);
const testing = ref(false);

const steps = ref<StepDraft[]>([]);
const datasourceList = ref<ApiDatasourceRecord[]>([]);

const testParams = ref('');
const testResult = ref<string | null>(null);
const testError = ref('');
const testCostMs = ref(0);

const statusText = computed(() => (status.value === 1 ? '已发布' : status.value === 2 ? '已停用' : '草稿'));
const statusColor = computed(() => (status.value === 1 ? 'green' : status.value === 2 ? 'red' : 'default'));

/** 模板表达式提示文案（脚本常量，避免模板内嵌 {{ }} 提前闭合插值） */
const exprHint = '{{步骤名.路径}}';
const paramsHint = '{{params.xxx}}';

const stepLabel = (t: StepDraft['type']) =>
  ({ sql: 'SQL', http: 'HTTP', map: '映射', save: '落库' })[t];
const stepColor = (t: StepDraft['type']) =>
  ({ sql: 'blue', http: 'purple', map: 'orange', save: 'green' })[t];

onMounted(async () => {
  try {
    const [detailRes, dsRes] = await Promise.all([
      getApiDetailApi(apiId),
      getDatasourceListApi().catch(() => ({ code: 500, data: [] as ApiDatasourceRecord[] })),
    ]);
    if (dsRes.code === 200 && dsRes.data) {
      datasourceList.value = dsRes.data;
    }
    if (detailRes.code !== 200 || !detailRes.data) {
      message.error(detailRes.msg || '接口加载失败');
      router.replace('/openapi/api');
      return;
    }
    const api: ApiRecord = detailRes.data;
    apiCode.value = api.apiCode;
    apiName.value = api.apiName;
    apiKey.value = api.apiKey ?? '';
    remark.value = api.remark ?? '';
    status.value = api.status;
    steps.value = parseStepsToDraft(api.steps ?? '[]');
  } catch {
    message.error('接口加载失败');
    router.replace('/openapi/api');
  }
});

/** 后端步骤 JSON → 编辑态草稿 */
function parseStepsToDraft(stepsJson: string): StepDraft[] {
  let list: StepDefItem[] = [];
  try {
    list = JSON.parse(stepsJson) as StepDefItem[];
  } catch {
    message.warning('步骤配置解析失败，已初始化为空');
    return [];
  }
  return list.map((s) => {
    const cfg = (s.config ?? {}) as Record<string, unknown>;
    const draft: StepDraft = {
      type: s.type,
      name: s.name,
      datasource: '',
      sqlText: '',
      paramText: '',
      url: '',
      method: 'GET',
      headerText: '',
      bodyText: '',
      mapSource: '',
      keepSource: false,
      fieldsText: '',
      saveSource: '',
    };
    if (s.type === 'sql') {
      draft.datasource = cfg.datasource ? String(cfg.datasource) : '';
      draft.sqlText = String(cfg.sql ?? '');
      draft.paramText = prettyJson(cfg.params);
    } else if (s.type === 'http') {
      draft.url = String(cfg.url ?? '');
      draft.method = String(cfg.method ?? 'GET').toUpperCase();
      draft.headerText = prettyJson(cfg.headers);
      draft.paramText = prettyJson(cfg.params);
      draft.bodyText = prettyJson(cfg.body);
    } else if (s.type === 'map') {
      draft.mapSource = String(cfg.source ?? '');
      draft.keepSource = cfg.keepSource === true;
      draft.fieldsText = prettyJson(cfg.fields);
    } else if (s.type === 'save') {
      draft.saveSource = String(cfg.source ?? '');
    }
    return draft;
  });
}

function prettyJson(v: unknown): string {
  if (v === undefined || v === null) return '';
  try {
    return JSON.stringify(v, null, 0);
  } catch {
    return '';
  }
}

/** 解析编辑态 JSON 文本，非法时抛出带字段名的错误 */
function parseJsonField(text: string, field: string, fallback: unknown): unknown {
  if (!text.trim()) return fallback;
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(`${field} 不是合法 JSON`);
  }
}

/** 编辑态草稿 → 后端步骤 JSON（保存前调用，含校验） */
function buildStepsJson(): string {
  const list: StepDefItem[] = steps.value.map((s) => {
    let config: Record<string, unknown> = {};
    if (s.type === 'sql') {
      config = {
        sql: s.sqlText,
        params: parseJsonField(s.paramText, `步骤[${s.name}] 参数`, {}),
        ...(s.datasource ? { datasource: s.datasource } : {}),
      };
    } else if (s.type === 'http') {
      config = {
        url: s.url,
        method: s.method,
        headers: parseJsonField(s.headerText, `步骤[${s.name}] 请求头`, {}),
        ...(s.method === 'GET'
          ? { params: parseJsonField(s.paramText, `步骤[${s.name}] Query参数`, {}) }
          : { body: parseJsonField(s.bodyText, `步骤[${s.name}] 请求体`, {}) }),
      };
    } else if (s.type === 'map') {
      config = {
        source: s.mapSource,
        keepSource: s.keepSource,
        fields: parseJsonField(s.fieldsText, `步骤[${s.name}] 字段映射`, []),
      };
    } else if (s.type === 'save') {
      config = { source: s.saveSource };
    }
    return { type: s.type, name: s.name, config };
  });
  return JSON.stringify(list);
}

// ---------------- 步骤操作 ----------------
const addStep = ({ key }: { key: string }) => {
  const base: StepDraft = {
    type: key as StepDraft['type'],
    name: `step${steps.value.length + 1}`,
    datasource: '',
    sqlText: '',
    paramText: '',
    url: '',
    method: 'GET',
    headerText: '',
    bodyText: '',
    mapSource: '',
    keepSource: false,
    fieldsText: '',
    saveSource: '',
  };
  if (key === 'sql') base.sqlText = 'SELECT 1';
  if (key === 'save') base.saveSource = 'query';
  steps.value.push(base);
};

const removeStep = (idx: number) => {
  steps.value.splice(idx, 1);
};

const moveStep = (idx: number, delta: number) => {
  const target = idx + delta;
  if (target < 0 || target >= steps.value.length) return;
  const arr = steps.value;
  [arr[idx], arr[target]] = [arr[target], arr[idx]];
};

// ---------------- 保存 / 发布 / 测试 ----------------
const handleSave = async () => {
  if (!apiCode.value.trim() || !apiName.value.trim()) {
    message.warning('请填写接口编码与名称');
    return;
  }
  let stepsJson: string;
  try {
    stepsJson = buildStepsJson();
  } catch (e) {
    message.error((e as Error).message);
    return;
  }
  saving.value = true;
  try {
    const res = await saveApiApi(apiId, apiCode.value.trim(), apiName.value.trim(), apiKey.value.trim(), stepsJson, remark.value);
    if (res.code === 200) {
      message.success('保存成功！');
      status.value = 0;
    } else {
      message.error(res.msg || '保存失败');
    }
  } finally {
    saving.value = false;
  }
};

const handlePublish = async () => {
  const res = await publishApiApi(apiId);
  if (res.code === 200) {
    status.value = 1;
    message.success('发布成功！');
  } else {
    message.error(res.msg || '发布失败');
  }
};

const handleStop = async () => {
  const res = await stopApiApi(apiId);
  if (res.code === 200) {
    status.value = 2;
    message.success('停用成功！');
  } else {
    message.error(res.msg || '停用失败');
  }
};

const runTest = async () => {
  testError.value = '';
  testResult.value = null;
  const paramsText = testParams.value.trim();
  if (paramsText) {
    try {
      JSON.parse(paramsText);
    } catch {
      testError.value = '入参不是合法 JSON';
      return;
    }
  }
  testing.value = true;
  try {
    const res = await testApiApi(apiId, paramsText);
    if (res.code === 200 && res.data) {
      testCostMs.value = res.data.costMs ?? 0;
      testResult.value = JSON.stringify(res.data.data, null, 2);
    } else {
      testError.value = res.msg || '测试运行失败';
    }
  } finally {
    testing.value = false;
  }
};

const handleBack = () => {
  router.push('/openapi/api');
};
</script>

<style scoped>
.api-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--ant-color-bg-layout, #f5f5f5);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px;
  background: var(--ant-color-bg-container, #fff);
  border-bottom: 1px solid var(--ant-color-border-secondary, #f0f0f0);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-name-input {
  width: 220px;
}

.body {
  display: flex;
  flex: 1;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}

.editor-pane {
  flex: 1.6;
  overflow: auto;
}

.test-pane {
  flex: 1;
  overflow: auto;
  min-width: 320px;
}

.block {
  margin-bottom: 12px;
}

.step-card {
  border: 1px solid var(--ant-color-border-secondary, #e5e6eb);
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
  background: var(--ant-color-bg-container, #fff);
}

.step-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.step-type {
  flex-shrink: 0;
}

.step-name {
  width: 180px;
}

.step-field {
  margin-bottom: 8px;
}

.step-hint {
  font-size: 12px;
  font-weight: normal;
  color: var(--ant-color-text-tertiary, #999);
  margin-left: 8px;
}

.test-meta {
  margin: 8px 0 4px;
  font-size: 12px;
  color: var(--ant-color-text-tertiary, #999);
}

.test-result {
  margin-top: 10px;
}

.test-pre {
  max-height: 50vh;
  overflow: auto;
  background: var(--ant-color-fill-quaternary, #f6f6f6);
  border-radius: 6px;
  padding: 8px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
