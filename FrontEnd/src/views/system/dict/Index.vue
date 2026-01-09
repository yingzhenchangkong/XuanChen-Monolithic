<template>
  <a-card>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParamsIndex" :formItems="queryFormItemsIndex" @search="loadData" @reset="handleReset" />
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button type="primary" @click="handleAdd">
        <template #icon>
          <PlusOutlined />
        </template>新增
      </a-button>
    </div>
    <!--表格区域-->
    <a-table :dataSource="dataSource" :columns="columnsIndex" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange, type: 'radio' }" bordered
      rowKey="id" size="small" @change="handleTableChange">
      <template #bodyCell="{ column, text, record }">
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
          <a-tag :color="record.status === true ? 'green' : 'volcano'">
            {{ record.status === true ? '启用' : '停用' }}
          </a-tag>
        </template>
      </template>
    </a-table>
    <!-- 子项 -->
    <a-tabs defaultActiveKey="1">
      <a-tab-pane tab="字典明细" key="1">
        <Item :mainId="mainId" />
      </a-tab-pane>
    </a-tabs>
    <!--弹窗区域-->
    <OperationIndex ref="refOperation" :operationTitle="operationTitle" @childOK="handleReset" />
  </a-card>
</template>

<script lang="ts" setup>
import { useList } from '@/hooks/useList';
import { ref, onMounted } from 'vue';
import { deleteAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';
import OperationIndex from './modal/OperationIndex.vue';
import Item from './Item.vue';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { queryParamsIndex, queryFormItemsIndex, columnsIndex } from './dict.data';

/** url */
const url = {
  list: '/system/dict/list',
  delete: '/system/dict/delete',
  deleteBatch: '/system/dict/deleteBatch',
}
const mainId = ref('');

/** 搜索 */
const handleSearch = () => {
  loadData();
  mainId.value = '';
}
/** 重置 */
const handleReset = () => {
  queryParamsIndex.dictName = '';
  mainId.value = '';
  loadData();
}
/** 删除 */
const handleDelete = (id: string) => {
  if (!url.delete) {
    message.error('请设置url.delete属性!')
    return
  }
  deleteAction(url.delete, { id: id }).then((res: any) => {
    if (res.code == 200) {
      message.success(res.msg);
      handleReset();
    } else {
      message.warning(res.msg);
    }
  })
}
/** 选择行改变时 改变已选择行的数组 */
const onSelectChange = (selectedRowKeys: any, selectionRows: any) => {
  state.selectedRowKeys = selectedRowKeys;
  mainId.value = selectionRows[0]['dictCode'];
};
const queryParams = queryParamsIndex;
const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit,
  dataSource, loading, ipagination, handleTableChange, state
} = useList({ url, queryParams })
loadData();
onMounted(() => {
  ipagination.pageSize = 5;
});
</script>