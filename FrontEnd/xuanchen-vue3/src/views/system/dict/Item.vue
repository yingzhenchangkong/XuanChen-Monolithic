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
import { message } from 'ant-design-vue';

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
  list: DictApiUrl.DICT_ITEM_LIST,
  delete: DictApiUrl.DICT_ITEM_DELETE,
}

/** 重置 */
const handleReset = () => {
  queryParamsItem.dictCode = ''
  loadData()
}

const handleStatusChange = async (record: Record<string, unknown>, index: number) => {
  const oldStatus = dataSource.value[index].status;
  const newStatus = record.status === 1 ? 0 : 1;
  // 乐观更新先翻 UI；业务失败或网络异常必须回滚，避免界面状态与数据库相反
  dataSource.value[index].status = newStatus;
  try {
    const res = await changeStatusItemApi(String(dataSource.value[index].id ?? ''), newStatus);
    if (res.code === 200) {
      message.success(res.msg);
    } else {
      dataSource.value[index].status = oldStatus;
      message.error(res.msg);
    }
  } catch {
    dataSource.value[index].status = oldStatus;
    // HTTP/网络错误已由响应拦截器统一提示
  }
}

const queryParams = queryParamsItem;
const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange
} = useList({ url, queryParams })
loadData()
</script>