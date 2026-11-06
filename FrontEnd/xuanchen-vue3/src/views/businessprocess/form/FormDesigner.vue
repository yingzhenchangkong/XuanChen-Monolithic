<template>
  <div class="form-designer">
    <!-- 顶部工具栏 -->
    <div class="fd-toolbar">
      <a-button @click="handleBack">
        <ArrowLeftOutlined /> 返回
      </a-button>
      <span class="fd-title">{{ formName || '表单设计' }}</span>
      <div class="fd-spacer"></div>
      <a-tag v-if="dirty" color="orange">有未保存更改</a-tag>
      <a-button @click="openPreview">
        <EyeOutlined /> 预览
      </a-button>
      <a-button type="primary" :loading="saving" @click="handleSave">
        <SaveOutlined /> 保存
      </a-button>
    </div>

    <div class="fd-body">
      <!-- 左侧控件面板 -->
      <div class="fd-palette">
        <div class="fd-panel-title">基础控件</div>
        <div class="fd-palette-grid">
          <div v-for="item in PALETTE" :key="item.type" class="fd-palette-item" draggable="true"
            @dragstart="onPaletteDragStart($event, item)">
            <component :is="item.icon" />
            <span>{{ item.label }}</span>
          </div>
        </div>
        <div class="fd-palette-tip">拖拽控件到中间画布</div>
      </div>

      <!-- 中间画布 -->
      <div class="fd-canvas" :class="{ 'drop-active': canvasDropActive }"
        @dragover.prevent="canvasDropActive = true" @dragleave="canvasDropActive = false"
        @drop.prevent="onCanvasDrop">
        <div v-if="!fields.length" class="fd-empty">
          <InboxOutlined style="font-size: 40px; color: #c9cdd4" />
          <p>从左侧拖入控件开始设计表单</p>
          <p class="fd-empty-tip">控件默认半行宽（多列并排），选中后可在右侧调整占列；多行输入默认整行</p>
        </div>
        <div v-else class="fd-canvas-inner">
          <div v-for="(field, idx) in fields" :key="field.uid" class="fd-field"
            :class="{ selected: selectedUid === field.uid, 'drag-over': dragOverIndex === idx }"
            :style="{ gridColumn: `span ${spanOf(field)}` }" draggable="true"
            @click="selectedUid = field.uid" @dragstart="onFieldDragStart($event, idx)"
            @dragover.prevent="dragOverIndex = idx" @dragleave="dragOverIndex = -1"
            @drop.stop.prevent="onFieldDrop($event, idx)">
          <div class="fd-field-label">
            {{ field.label }}<span v-if="field.required" class="fd-required">*</span>
          </div>
          <!-- 控件示意 -->
          <a-input v-if="field.type === 'input'" disabled placeholder="单行输入" />
          <a-textarea v-else-if="field.type === 'textarea'" disabled placeholder="多行输入" :rows="2" />
          <a-input-number v-else-if="field.type === 'number'" disabled placeholder="数字" style="width: 100%" />
          <a-date-picker v-else-if="field.type === 'date'" disabled placeholder="日期选择" style="width: 100%" />
          <a-select v-else-if="field.type === 'select'" disabled placeholder="下拉选择"
            :options="(field.options ?? []).map((o) => ({ label: o, value: o }))" />
          <a-radio-group v-else-if="field.type === 'radio'" disabled>
            <a-radio v-for="opt in field.options ?? []" :key="opt" :value="opt">{{ opt }}</a-radio>
          </a-radio-group>
          <a-checkbox-group v-else-if="field.type === 'checkbox'" disabled
            :options="(field.options ?? []).map((o) => ({ label: o, value: o }))" />
          <a-switch v-else-if="field.type === 'switch'" disabled />
          <div class="fd-field-actions">
            <a title="上移" @click.stop="moveField(idx, -1)">
              <ArrowUpOutlined />
            </a>
            <a title="下移" @click.stop="moveField(idx, 1)">
              <ArrowDownOutlined />
            </a>
            <a class="fd-danger" title="删除" @click.stop="removeField(idx)">
              <DeleteOutlined />
            </a>
          </div>
          </div>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="fd-props">
        <template v-if="selected">
          <div class="fd-panel-title">控件属性</div>
          <a-form layout="vertical" class="fd-props-form">
            <a-form-item label="标签" required>
              <a-input v-model:value="selected.label" :maxlength="30" @change="dirty = true" />
            </a-form-item>
            <a-form-item label="字段名（提交 key）" required>
              <a-input v-model:value="selected.key" :maxlength="50" placeholder="如 amount"
                @change="dirty = true" />
            </a-form-item>
            <a-form-item label="提示文字">
              <a-input v-model:value="selected.placeholder" :maxlength="100" placeholder="选填"
                @change="dirty = true" />
            </a-form-item>
            <a-form-item label="占列宽度">
              <a-select v-model:value="selected.span" @change="dirty = true">
                <a-select-option :value="24">整行</a-select-option>
                <a-select-option :value="18">3/4 行</a-select-option>
                <a-select-option :value="16">2/3 行</a-select-option>
                <a-select-option :value="12">半行（一行两个）</a-select-option>
                <a-select-option :value="8">1/3 行（一行三个）</a-select-option>
                <a-select-option :value="6">1/4 行（一行四个）</a-select-option>
              </a-select>
              <div class="fd-panel-hint">同一行的控件按占列宽度从左往右排列，放不下自动换行。</div>
            </a-form-item>
            <a-form-item>
              <a-checkbox v-model:checked="selected.required" @change="dirty = true">必填</a-checkbox>
            </a-form-item>
            <a-form-item v-if="hasOptions(selected.type)" label="选项（每行一个）">
              <a-textarea :value="(selected.options ?? []).join('\n')" :rows="4"
                @change="(e: Event) => onOptionsChange(e, selected!)" />
            </a-form-item>
          </a-form>
        </template>
        <div v-else class="fd-panel-empty">
          <p>选中画布中的控件后在此配置属性。</p>
          <p>拖拽画布中的控件可调整顺序。</p>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <a-modal v-model:open="previewOpen" title="表单预览" width="720px" :footer="null">
      <XCFormRender :schema="previewSchema" :model-value="previewModel" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  AlignLeftOutlined, ArrowDownOutlined, ArrowLeftOutlined, ArrowUpOutlined, CalendarOutlined,
  CheckCircleOutlined, CheckSquareOutlined, DeleteOutlined, EditOutlined, EyeOutlined,
  InboxOutlined, NumberOutlined, SaveOutlined, SwapOutlined, UnorderedListOutlined,
} from '@ant-design/icons-vue';

import XCFormRender from '@/components/xuanchen/XCFormRender.vue';
import {
  getFormDetailApi, saveFormApi,
} from './form.api';
import type { FormFieldSchema, FormFieldType } from '../businessprocess.types';

const route = useRoute();
const router = useRouter();

const formId = route.params.id as string;
const formName = ref('');
const remark = ref('');
const saving = ref(false);
const dirty = ref(false);

/** 字段设计（带 uid 供拖拽/选中定位，保存时剔除） */
interface DesignerField extends FormFieldSchema {
  uid: string;
}

const PALETTE: Array<{ type: FormFieldType; label: string; icon: unknown }> = [
  { type: 'input', label: '单行输入', icon: EditOutlined },
  { type: 'textarea', label: '多行输入', icon: AlignLeftOutlined },
  { type: 'number', label: '数字', icon: NumberOutlined },
  { type: 'date', label: '日期', icon: CalendarOutlined },
  { type: 'select', label: '下拉选择', icon: UnorderedListOutlined },
  { type: 'radio', label: '单选', icon: CheckCircleOutlined },
  { type: 'checkbox', label: '多选', icon: CheckSquareOutlined },
  { type: 'switch', label: '开关', icon: SwapOutlined },
];

const fields = ref<DesignerField[]>([]);
const selectedUid = ref('');
const selected = computed(() => fields.value.find((f) => f.uid === selectedUid.value) ?? null);

let uidSeq = 1;
const nextUid = () => `f${Date.now()}_${uidSeq++}`;
const hasOptions = (type: FormFieldType) => type === 'select' || type === 'radio' || type === 'checkbox';

const makeField = (type: FormFieldType): DesignerField => ({
  uid: nextUid(),
  type,
  label: PALETTE.find((p) => p.type === type)?.label ?? type,
  key: uniqueKey(type),
  placeholder: '',
  required: false,
  span: type === 'textarea' ? 24 : 12,
  options: hasOptions(type) ? ['选项1', '选项2', '选项3'] : undefined,
});

/** 设计器画布占列（与 XCFormRender.spanOf 同规则） */
const spanOf = (field: DesignerField) => {
  const raw = Number(field.span ?? (field.type === 'textarea' ? 24 : 12));
  return Math.min(24, Math.max(1, Number.isFinite(raw) ? Math.round(raw) : 24));
};

/** 生成不重复的字段名：类型前缀 + 序号 */
const uniqueKey = (type: FormFieldType) => {
  const used = new Set(fields.value.map((f) => f.key));
  let seq = fields.value.length + 1;
  let key = `${type}${seq}`;
  while (used.has(key)) {
    seq += 1;
    key = `${type}${seq}`;
  }
  return key;
};

// ---------- 拖拽：新增 ----------
const canvasDropActive = ref(false);
const dragOverIndex = ref(-1);
/** 当前拖拽来源：palette=新增控件，move=画布内调整顺序 */
let dragSource: { kind: 'palette'; type: FormFieldType } | { kind: 'move'; from: number } | null = null;

const onPaletteDragStart = (e: DragEvent, item: { type: FormFieldType }) => {
  dragSource = { kind: 'palette', type: item.type };
  e.dataTransfer?.setData('text/plain', item.type);
};

const onFieldDragStart = (e: DragEvent, idx: number) => {
  dragSource = { kind: 'move', from: idx };
  e.dataTransfer?.setData('text/plain', String(idx));
};

const onCanvasDrop = () => {
  canvasDropActive.value = false;
  if (dragSource?.kind === 'palette') {
    fields.value.push(makeField(dragSource.type));
    selectedUid.value = fields.value[fields.value.length - 1].uid;
    dirty.value = true;
  } else if (dragSource?.kind === 'move') {
    // 落到画布空白处 = 移到末尾
    moveFieldTo(dragSource.from, fields.value.length - 1);
    dirty.value = true;
  }
  dragSource = null;
  dragOverIndex.value = -1;
};

const onFieldDrop = (_e: DragEvent, idx: number) => {
  if (dragSource?.kind === 'palette') {
    fields.value.splice(idx, 0, makeField(dragSource.type));
    selectedUid.value = fields.value[idx].uid;
    dirty.value = true;
  } else if (dragSource?.kind === 'move') {
    moveFieldTo(dragSource.from, idx);
    dirty.value = true;
  }
  dragSource = null;
  dragOverIndex.value = -1;
};

const moveFieldTo = (from: number, to: number) => {
  if (from === to || from < 0 || from >= fields.value.length) return;
  const [item] = fields.value.splice(from, 1);
  fields.value.splice(to, 0, item);
};

const moveField = (idx: number, delta: number) => {
  const to = idx + delta;
  if (to < 0 || to >= fields.value.length) return;
  moveFieldTo(idx, to);
  dirty.value = true;
};

const removeField = (idx: number) => {
  if (fields.value[idx].uid === selectedUid.value) {
    selectedUid.value = '';
  }
  fields.value.splice(idx, 1);
  dirty.value = true;
};

const onOptionsChange = (e: Event, field: DesignerField) => {
  const text = (e.target as HTMLTextAreaElement).value;
  field.options = text.split('\n').map((s) => s.trim()).filter(Boolean);
  dirty.value = true;
};

// ---------- 加载 / 保存 ----------
const parseSchema = (schemaJson?: string): DesignerField[] => {
  if (!schemaJson) return [];
  try {
    const obj = JSON.parse(schemaJson);
    if (!Array.isArray(obj?.fields)) return [];
    return obj.fields.map((f: FormFieldSchema) => ({
      ...f,
      uid: nextUid(),
      span: f.span ?? (f.type === 'textarea' ? 24 : 12),
    }));
  } catch {
    return [];
  }
};

const serialize = () =>
  JSON.stringify({
    fields: fields.value.map(({ uid: _uid, ...rest }) => rest),
  });

onMounted(async () => {
  const res = await getFormDetailApi(formId);
  if (res.code !== 200 || !res.data) {
    message.error(res.msg || '加载表单失败');
    return;
  }
  formName.value = res.data.formName ?? '';
  remark.value = res.data.remark ?? '';
  fields.value = parseSchema(res.data.schemaJson);
});

const handleSave = async () => {
  if (!formName.value.trim()) {
    message.warning('请输入表单名称');
    return;
  }
  // 字段名重复校验
  const keys = fields.value.map((f) => f.key.trim()).filter(Boolean);
  if (new Set(keys).size !== keys.length) {
    message.warning('存在重复的字段名，请修改后保存');
    return;
  }
  if (fields.value.some((f) => !f.key.trim() || !f.label.trim())) {
    message.warning('控件的标签和字段名不能为空');
    return;
  }
  saving.value = true;
  try {
    const res = await saveFormApi(formId, formName.value.trim(), remark.value.trim(), serialize());
    if (res.code === 200) {
      dirty.value = false;
      message.success('保存成功！');
    } else {
      message.error(res.msg || '保存失败');
    }
  } finally {
    saving.value = false;
  }
};

const handleBack = () => {
  if (dirty.value && !window.confirm('有未保存的更改，确定离开吗？')) return;
  router.push('/businessprocess/form');
};

// ---------- 预览 ----------
const previewOpen = ref(false);
const previewSchema = computed(() => serialize());
const previewModel = ref<Record<string, unknown>>({});

const openPreview = () => {
  previewModel.value = {};
  previewOpen.value = true;
};
</script>

<style scoped>
.form-designer {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f7;
}

.fd-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
}

.fd-title {
  font-weight: 500;
}

.fd-spacer {
  flex: 1;
}

.fd-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.fd-panel-title {
  font-size: 13px;
  color: #4e5969;
  margin-bottom: 10px;
}

/* 左侧控件面板 */
.fd-palette {
  width: 200px;
  padding: 12px;
  background: #fff;
  border-right: 1px solid #e5e6eb;
  overflow: auto;
}

.fd-palette-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.fd-palette-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  border: 1px dashed #d8dce6;
  border-radius: 6px;
  font-size: 12px;
  color: #4e5969;
  cursor: grab;
  user-select: none;
  background: #fafbfc;
}

.fd-palette-item:hover {
  border-color: #5f95ff;
  color: #165dff;
}

.fd-palette-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #86909c;
  text-align: center;
}

/* 中间画布 */
.fd-canvas {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.fd-canvas.drop-active {
  outline: 2px dashed #5f95ff;
  outline-offset: -6px;
}

.fd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 120px;
  color: #86909c;
}

.fd-empty-tip {
  font-size: 12px;
  color: #a9aeb8;
}

/* 画布内 24 列网格：控件按 span 占列，天然支持多列并排 */
.fd-canvas-inner {
  display: grid;
  grid-template-columns: repeat(24, minmax(0, 1fr));
  gap: 12px;
  max-width: 960px;
  margin: 0 auto;
}

.fd-field {
  position: relative;
  padding: 12px 40px 12px 16px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  cursor: grab;
}

.fd-field.selected {
  border-color: #5f95ff;
  box-shadow: 0 0 0 2px rgba(95, 149, 255, 0.15);
}

.fd-field.drag-over {
  border-top: 2px solid #5f95ff;
}

.fd-field-label {
  font-size: 13px;
  margin-bottom: 6px;
}

.fd-required {
  color: #f53f3f;
  margin-left: 2px;
}

.fd-field-actions {
  position: absolute;
  right: 8px;
  top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #86909c;
}

.fd-field-actions a {
  color: inherit;
}

.fd-field-actions a:hover {
  color: #165dff;
}

.fd-field-actions .fd-danger:hover {
  color: #f53f3f;
}

/* 右侧属性面板 */
.fd-props {
  width: 280px;
  padding: 12px;
  background: #fff;
  border-left: 1px solid #e5e6eb;
  overflow: auto;
}

.fd-props-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.fd-panel-empty {
  color: #86909c;
  font-size: 13px;
  padding-top: 24px;
}

.fd-panel-hint {
  font-size: 12px;
  color: #a9aeb8;
  margin-top: 4px;
  line-height: 1.6;
}

.fd-props :deep(.ant-select) {
  width: 100%;
}
</style>
