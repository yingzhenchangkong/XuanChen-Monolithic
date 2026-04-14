<template>
  <a-table :columns="columnsUI" :data-source="dataSource" :pagination="false">
    <template #bodyCell="{ column, text, record, index }">
      <template v-if="column.dataIndex === 'uiIsQuery'">
        <Checkbox v-model:checked="record.uiIsQuery" />
      </template>
      <template v-else-if="column.dataIndex === 'uiQueryType'">
        <a-select :disabled="!record.uiIsQuery" v-model:value="record.uiQueryType" :options="optionsControlType"
          style="width: 100%" :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择查询方式"
          allowClear></a-select>
      </template>
      <template v-else-if="column.dataIndex === 'uiIsEdit'">
        <Checkbox v-model:checked="record.uiIsEdit" />
      </template>
      <template v-else-if="column.dataIndex === 'uiIsRequired'">
        <Checkbox v-model:checked="record.uiIsRequired" />
      </template>
      <template v-else-if="column.dataIndex === 'uiEditType'">
        <a-select :disabled="!record.uiIsEdit" v-model:value="record.uiEditType" :options="optionsControlType"
          style="width: 100%" :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择编辑方式"
          allowClear></a-select>
      </template>
      <template v-else-if="column.dataIndex === 'uiIsList'">
        <Checkbox v-model:checked="record.uiIsList" />
      </template>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { Checkbox } from 'ant-design-vue';
import { columnsUI } from '../gentable.data';
import type { GenTableColumn } from '../gentable.types';
import { getDictSelect } from '@/views/system/dict/dict.api';

const emit = defineEmits(['childData']);
const dataSource = reactive<GenTableColumn[]>([]);

const init = (data: GenTableColumn[]) => {
  dataSource.splice(0, dataSource.length, ...data);
};

const optionsControlType = ref([]);
const getControlType = async () => {
  const res: any = await getDictSelect('ui_control_type');
  optionsControlType.value = res;
}
getControlType();

defineExpose({
  init,
});
</script>