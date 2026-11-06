<template>
  <a-form layout="vertical" class="xc-form-render" :class="{ 'is-disabled': disabled }">
    <a-row :gutter="16">
      <a-col v-for="field in fields" :key="field.key" :span="spanOf(field)">
        <a-form-item :label="disabled ? field.label : `${field.label}${field.required ? ' *' : ''}`">
          <!-- 只读态（审批人查看）：统一文本展示 -->
          <template v-if="disabled">
            <span class="xc-form-render-text">{{ displayValue(field) }}</span>
          </template>
          <!-- 编辑态 -->
          <template v-else>
            <a-input v-if="field.type === 'input'" v-model:value="model[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`" :maxlength="500" allowClear />
            <a-textarea v-else-if="field.type === 'textarea'" v-model:value="model[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`" :rows="3" :maxlength="2000" />
            <a-input-number v-else-if="field.type === 'number'" v-model:value="model[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`" style="width: 100%" />
            <a-date-picker v-else-if="field.type === 'date'" v-model:value="model[field.key]"
              value-format="YYYY-MM-DD" style="width: 100%" :placeholder="field.placeholder || '请选择日期'" />
            <a-select v-else-if="field.type === 'select'" v-model:value="model[field.key]"
              :placeholder="field.placeholder || `请选择${field.label}`" :options="optionItems(field)" allowClear />
            <a-radio-group v-else-if="field.type === 'radio'" v-model:value="model[field.key]">
              <a-radio v-for="opt in optionItems(field)" :key="String(opt.value)" :value="opt.value">{{ opt.label }}</a-radio>
            </a-radio-group>
            <a-checkbox-group v-else-if="field.type === 'checkbox'" v-model:value="model[field.key]"
              :options="optionItems(field)" />
            <a-switch v-else-if="field.type === 'switch'" v-model:checked="model[field.key]" />
          </template>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup lang="ts">
/**
 * 在线表单渲染器（XC 约定组件）
 * 输入：表单设计 schema（fields 数组）+ 数据对象（v-model:value）。
 * 发起申请页编辑渲染；审批详情传 disabled=true 只读展示。
 */
import { computed } from 'vue';
import type { FormFieldSchema, FormSchema } from '@/views/businessprocess/businessprocess.types';

const props = defineProps<{
  /** 表单设计 JSON 字符串或已解析对象 */
  schema: string | FormSchema | null | undefined;
  /** 数据对象（字段 key -> 值），v-model:value */
  modelValue: Record<string, unknown>;
  /** 只读展示（审批详情） */
  disabled?: boolean;
}>();

const emit = defineEmits<{ (e: 'update:modelValue', value: Record<string, unknown>): void }>();

const parsed = computed<FormSchema>(() => {
  if (!props.schema) return { fields: [] };
  if (typeof props.schema === 'string') {
    try {
      const obj = JSON.parse(props.schema);
      return Array.isArray(obj?.fields) ? obj : { fields: [] };
    } catch {
      return { fields: [] };
    }
  }
  return props.schema;
});

const fields = computed(() => parsed.value.fields.filter((f) => f.key && f.label));

/** 就地修改传入对象（与 XCQueryForm 同约定，不派发 update:modelValue 也可用） */
const model = props.modelValue;

/** 占列：优先用设计的 span（钳制 1~24），旧数据按类型兜底（textarea 整行，其余半行） */
const spanOf = (field: FormFieldSchema) => {
  const raw = Number(field.span ?? (field.type === 'textarea' ? 24 : 12));
  return Math.min(24, Math.max(1, Number.isFinite(raw) ? Math.round(raw) : 24));
};

const optionItems = (field: FormFieldSchema) =>
  (field.options ?? []).map((opt) => ({ label: opt, value: opt }));

/** 只读态的展示文本 */
const displayValue = (field: FormFieldSchema): string => {
  const value = model[field.key];
  if (value === undefined || value === null || value === '') return '-';
  if (field.type === 'switch') return value ? '是' : '否';
  if (field.type === 'checkbox' && Array.isArray(value)) return value.length ? value.join('、') : '-';
  if (Array.isArray(value)) return value.join('、');
  return String(value);
};

/** 提交前收集有效值：过滤空串/空数组，返回 formData */
const collect = (): Record<string, unknown> => {
  const data: Record<string, unknown> = {};
  for (const field of fields.value) {
    const value = model[field.key];
    if (value === undefined || value === null || value === '') continue;
    if (Array.isArray(value) && value.length === 0) continue;
    data[field.key] = value;
  }
  return data;
};

/** 必填校验，返回第一个缺失字段标签；全部通过返回 null */
const validate = (): string | null => {
  for (const field of fields.value) {
    if (!field.required) continue;
    const value = model[field.key];
    if (value === undefined || value === null || value === '') return field.label;
    if (Array.isArray(value) && value.length === 0) return field.label;
  }
  return null;
};

/** 清空所有字段值 */
const reset = () => {
  for (const field of fields.value) {
    delete model[field.key];
  }
  emit('update:modelValue', { ...model });
};

defineExpose({ collect, validate, reset });
</script>

<style scoped>
.xc-form-render-text {
  color: #1d2129;
  word-break: break-all;
}

.xc-form-render.is-disabled :deep(.ant-form-item) {
  margin-bottom: 8px;
}
</style>
