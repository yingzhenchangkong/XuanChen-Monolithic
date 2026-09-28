<template>
  <a-select v-model:value="modelValue" style="width: 100%" :options="options" :field-names="computedFieldNames"
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
  filterOption: { type: Function, default: undefined }
})

const emit = defineEmits(['update:modelValue'])

const modelValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 计算字段映射，为用户数据提供默认值
const computedFieldNames = computed(() => {
  // 如果是用户相关接口，使用默认的用户字段映射
  if (props.apiUrl.includes('/user/') || props.apiUrl.includes('user')) {
    return props.fieldNames?.label ? props.fieldNames : { label: 'nickName', value: 'userName' };
  }
  return props.fieldNames;
});

const options = ref<SelectOption[]>([]);

// 默认过滤函数
const defaultFilterOption = (input: string, option: any) => {
  const labelField = computedFieldNames.value.label || 'label';
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

// 获取数据
const fetchData = async () => {
  // 如果有依赖值但为空，则清空选项
  if (props.watchValue !== undefined && !props.watchValue) {
    options.value = [];
    return;
  }

  try {
    const res = await getAction<SelectOption[]>(props.apiUrl, {
      ...props.params,
      ...(props.watchValue !== undefined ? { [Object.keys(props.params).find(key => props.params[key] === '${watchValue}') || '']: props.watchValue } : {})
    });

    if (res.code === 200) {
      options.value = res.data;
    }
  } catch (error) {
    console.error('获取数据失败:', error);
    options.value = [];
  }
};

// 监听依赖值变化（immediate 已覆盖组件初始化：无论 watchValue 是否传入，挂载时都会执行一次）
watch(() => props.watchValue, () => {
  fetchData();
}, { immediate: true });

// 监听参数变化（非 immediate，避免与上面的 immediate watch 在挂载时重复请求）
watch(() => props.params, () => {
  fetchData();
}, { deep: true });

// 注意：不要再在 onMounted 里调 fetchData——immediate watch 挂载时已触发一次，
// 两个入口并存会导致每个 XCSelect 挂载即发两次完全相同的请求
</script>