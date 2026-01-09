<template>
  <div class="query-style btn-style">
    <a-form layout="inline" :model="modelValue" @keyup.enter="$emit('search')">
      <a-form-item v-for="item in formItems" :key="item.name" :label="item.label">
        <a-input v-if="item.type === 'input'" v-model:value="modelValue[item.name]" :placeholder="item.placeholder"
          allowClear />
        <a-select v-else-if="item.type === 'select'" v-model:value="modelValue[item.name]"
          :style="{ width: item.width || '150px' }" :options="item.options" :fieldNames="item.fieldNames"
          :placeholder="item.placeholder" allowClear />
      </a-form-item>
      <a-button type="primary" @click="$emit('search')">
        <template #icon>
          <SearchOutlined />
        </template>查询
      </a-button>
      <a-button @click="$emit('reset')">
        <template #icon>
          <ReloadOutlined />
        </template>重置
      </a-button>
    </a-form>
  </div>
</template>

<script setup lang="ts">
interface FormItem {
  name: string;
  label: string;
  type: 'input' | 'select';
  placeholder?: string;
  width?: string;
  options?: any[];
  fieldNames?: { label: string; value: string };
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