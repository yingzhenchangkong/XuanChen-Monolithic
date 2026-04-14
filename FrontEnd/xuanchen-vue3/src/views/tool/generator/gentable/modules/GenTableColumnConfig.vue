<template>
  <div class="btn-style">
    <a-button type="primary" @click="handleAdd">
      <template #icon>
        <PlusOutlined />
      </template>新增
    </a-button>
  </div>
  <a-table :columns="columnsTable" :data-source="dataSource" :pagination="false">
    <template #bodyCell="{ column, text, record, index }">
      <template v-if="column.dataIndex === 'columnName'">
        <a-input v-model:value="record.columnName" placeholder="请输入字段名称" />
      </template>
      <template v-else-if="column.dataIndex === 'columnType'">
        <a-select v-model:value="record.columnType" :options="optionsColumnType" style="width: 120px"
          :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择字段类型" allowClear></a-select>
      </template>
      <template v-else-if="column.dataIndex === 'columnComment'">
        <a-input v-model:value="record.columnComment" placeholder="请输入字段注释" />
      </template>
      <template v-else-if="column.dataIndex === 'columnLength'">
        <a-input v-model:value="record.columnLength" placeholder="请输入字段长度" />
      </template>
      <template v-else-if="column.dataIndex === 'columnScale'">
        <a-input v-model:value="record.columnScale" placeholder="请输入字段精度" />
      </template>
      <template v-else-if="column.dataIndex === 'columnDefault'">
        <a-input v-model:value="record.columnDefault" placeholder="请输入字段默认值" />
      </template>
      <template v-else-if="column.dataIndex === 'columnIsPk'">
        <Checkbox v-model:checked="record.columnIsPk" />
      </template>
      <template v-else-if="column.dataIndex === 'columnIsNullable'">
        <Checkbox v-model:checked="record.columnIsNullable" />
      </template>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { Checkbox } from 'ant-design-vue';
import { columnsTable } from '../gentable.data';
import type { GenTableColumn } from '../gentable.types';
import { getDictSelect } from '@/views/system/dict/dict.api';

const emit = defineEmits(['childData']);
const dataSource = reactive<GenTableColumn[]>([]);
const init = (data: GenTableColumn[]) => {
  dataSource.splice(0, dataSource.length, ...data);
};

const optionsColumnType = ref([]);
const getColumnType = async () => {
  const res: any = await getDictSelect('db_table_column_type');
  optionsColumnType.value = res;
}
getColumnType();

const handleAdd = () => {
  dataSource.push({
    id: '',
    tableId: '',
    columnName: '',
    columnComment: '',
    columnType: undefined,
    columnLength: '',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: false,
    uiIsRequired: false,
    uiIsEdit: false,
    uiIsList: false,
    uiIsQuery: false,
    uiQueryType: undefined,
  });
  emit('childData', dataSource);
};

defineExpose({
  init,
});
</script>