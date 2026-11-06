<template>
  <!--操作按钮区域-->
  <div class="btn-style">
    <a-button type="primary" @click="handleAdd" v-if="props.mainId">
      <template #icon>
        <PlusOutlined />
      </template>新增
    </a-button>
  </div>
  <!--表格区域-->
  <a-table :dataSource="dataSource" :columns="columnsItem" :pagination="ipagination" :loading="loading"
    :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange, type: 'radio' }" bordered
    rowKey="id" size="small" @change="handleTableChange">
    <template #bodyCell="{ column, text, record, index }">
      <template v-if="column.dataIndex === 'operation'">
        <a @click="handleEdit(record)">
          <EditOutlined /> 编辑
        </a>
        <a-divider type="vertical"></a-divider>
        <a-popconfirm title="确定删除吗？" @confirm="() => handleDelete(record.id)" placement="left">
          <a>
            <DeleteOutlined /> 删除
          </a>
        </a-popconfirm>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="record.status === 1 ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
          @click="handleStatusChange(record, index)">
          {{ dataSource[index].status === 1 ? '启用' : '停用' }}
        </a-tag>
      </template>
    </template>
  </a-table>
  <!--弹窗区域-->
  <OperationItem ref="refOperation" :operationTitle="operationTitle" :dictCode="props.mainId" @childOK="loadData" />
</template>

<script lang="ts" setup>
import { watch } from 'vue';
import { useList } from '@/hooks/useList';
import { useStatusToggle } from '@/hooks/useStatusToggle';

import OperationItem from './modal/OperationItem.vue';

import { DictApiUrl, changeStatusItemApi } from './dict.api';
import { queryParamsItem, columnsItem } from './dict.data';

const props = defineProps({
  mainId: String
})

watch(() => props.mainId, (newVal, oldVal) => {
  if (!props.mainId) {
    handleReset();
  } else {
    queryParamsItem.dictCode = props.mainId;
    loadData();
  }
})

/** url */
const url = {
  list: DictApiUrl.INDEX_ITEM_LIST,
  delete: DictApiUrl.INDEX_ITEM_DELETE,
}

/** 重置 */
const handleReset = () => {
  queryParamsItem.dictCode = ''
  loadData()
}

const queryParams = queryParamsItem;
const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange
} = useList({ url, queryParams })
loadData()

/** 状态列切换：复用通用 hook（乐观更新 + 失败回滚），不再各页重复实现 */
const { handleStatusChange } = useStatusToggle({
  dataSource,
  changeApi: changeStatusItemApi,
})
</script>
