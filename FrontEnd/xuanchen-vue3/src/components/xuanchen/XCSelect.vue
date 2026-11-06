<template>
  <a-select v-model:value="modelValue" style="width: 100%" :options="options" :field-names="props.fieldNames"
    :placeholder="placeholder" :allow-clear="allowClear" :disabled="disabled" :mode="mode" :show-search="showSearch"
    :filter-option="filterOption">
  </a-select>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { getAction } from '@/utils/httpAction';
import type { SelectOption } from '@/types/api';

const props = defineProps({
  modelValue: { type: [String, Number, Array], default: undefined },
  // API地址
  apiUrl: { type: String, required: true },
  // 请求参数
  params: { type: Object, default: () => ({}) },
  // 监听变化的依赖值
  watchValue: { type: [String, Number], default: undefined },
  // watchValue 对应的请求参数名
  watchParamName: { type: String, default: 'watchValue' },
  // 字段映射配置
  fieldNames: {
    type: Object,
    default: () => ({ label: 'label', value: 'value' })
  },
  // 占位符文本
  placeholder: { type: String, default: '请选择' },
  // 是否允许清除
  allowClear: { type: Boolean, default: true },
  // 是否禁用
  disabled: { type: Boolean, default: false },
  // 选择模式
  mode: { type: String, default: undefined },
  // 是否显示搜索框
  showSearch: { type: Boolean, default: false },
  // 自定义过滤函数
  filterOption: { type: Function, default: undefined },
  // 响应数据路径，支持从嵌套结构中提取选项数组（如分页接口返回 res.data.records）
  // 默认取 res.data；设为 'data.records' 则取 res.data.records
  dataPath: { type: String, default: 'data' },
  // 选项展示文案格式化函数：(原始记录) => string，返回值写入 fieldNames.label 对应字段
  labelFormatter: { type: Function, default: undefined }
})

const emit = defineEmits(['update:modelValue'])

const modelValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const options = ref<SelectOption[]>([]);

const defaultFilterOption = (input: string, option: any) => {
  const labelField = props.fieldNames?.label || 'label';
  return option[labelField]?.includes(input);
};

// 使用的过滤函数
const filterOption = computed(() => {
  if (props.filterOption) {
    return props.filterOption;
  }
  if (props.showSearch) {
    return defaultFilterOption;
  }
  return undefined;
});

// 按 dataPath 从响应体中取出选项数组（支持 'data' / 'data.records' 等点分路径）
const extractData = (res: any): any[] => {
  const path = props.dataPath || 'data';
  const segments = path.split('.');
  let value = res;
  for (const seg of segments) {
    value = value?.[seg];
    if (value == null) return [];
  }
  return Array.isArray(value) ? value : [];
}

// 获取数据
const fetchData = async () => {
  // 如果有依赖值但为空（null 或空字符串），则清空选项；0 等 falsy 值视为有效
  if (props.watchValue !== undefined && (props.watchValue === null || props.watchValue === '')) {
    options.value = [];
    return;
  }

  try {
    const res = await getAction(props.apiUrl, {
      ...props.params,
      ...(props.watchValue !== undefined ? { [props.watchParamName]: props.watchValue } : {})
    });

    if (res.code === 200) {
      options.value = applyFormatter(extractData(res));
    }
  } catch (error) {
    console.error('获取数据失败:', error);
    options.value = [];
  }
};

// 应用展示文案格式化：把格式化结果写入 label 对应字段（value 等其余字段保持原样）
const applyFormatter = (list: any[]): SelectOption[] => {
  const formatter = props.labelFormatter;
  if (!formatter) return list;
  const labelField = props.fieldNames?.label || 'label';
  return list.map(item => ({ ...item, [labelField]: formatter(item) }));
};

// 监听依赖值与参数变化（合并为单个 watcher 避免两者同时变化时重复请求）
watch(
  [() => props.watchValue, () => props.params],
  () => { fetchData(); },
  { immediate: true, deep: true }
);
</script>