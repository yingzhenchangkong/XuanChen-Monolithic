<template>
  <a-form layout="inline" :model="modelValue" @keyup.enter="$emit('search')">
    <a-form-item v-for="item in formItems" :key="item.name" :label="item.label">
      <!-- input -->
      <a-input v-if="item.type === 'input'" v-model:value="modelValue[item.name]" :placeholder="item.placeholder"
        allowClear />
      <!-- select -->
      <a-select v-else-if="item.type === 'select'" v-model:value="modelValue[item.name]"
        :style="{ width: item.width || '150px' }" :options="item.options" :fieldNames="item.fieldNames"
        :placeholder="item.placeholder || '请选择'" allowClear />
      <!-- datePicker 后端实体类需加 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")-->
      <a-date-picker v-else-if="item.type === 'datePicker'" :show-time="item.showTime || false"
        v-model:value="modelValue[item.name]" :value-format="item.valueFormat || 'YYYY-MM-DD HH:mm:ss'"
        :format="item.displayFormat || 'YYYY-MM-DD HH:mm:ss'" :placeholder="item.placeholder || '请选择'"
        style="width: 100%;" allowClear />
      <!-- rangePicker -->
      <a-range-picker v-else-if="item.type === 'rangePicker'" :show-time="item.showTime || false"
        v-model:value="modelValue[item.name]" :value-format="item.valueFormat || 'YYYY-MM-DD HH:mm:ss'"
        :format="item.displayFormat || 'YYYY-MM-DD HH:mm:ss'" :placeholder="item.placeholder || ['开始时间', '结束时间']"
        style="width: 100%;" allowClear />
    </a-form-item>
    <a-form-item>
      <a-button type="primary" @click="$emit('search')">
        <template #icon>
          <SearchOutlined />
        </template>查询
      </a-button>
      <a-button @click="$emit('reset')" style="margin-left: 10px;">
        <template #icon>
          <ReloadOutlined />
        </template>重置
      </a-button>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
interface FormItem {
  name: string;
  label: string;
  type: 'input' | 'select' | 'datePicker' | 'rangePicker';
  placeholder?: string | string[];
  width?: string;
  options?: any[];
  fieldNames?: { label: string; value: string };
  showTime?: boolean;
  valueFormat?: string;
  displayFormat?: string;
}

defineProps<{
  modelValue: Record<string, any>;
  formItems: FormItem[];
}>();

defineEmits<{
  (e: 'search'): void;
  (e: 'reset'): void;
}>();
</script>