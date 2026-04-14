<template>
  <a-card>
    <!-- 查询区域 -->
    <XCQueryForm v-model="queryParams" :formItems="queryFormItems" @search="loadData" @reset="handleReset" />
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button type="primary" @click="handleAdd">
        <template #icon>
          <PlusOutlined />
        </template>新增
      </a-button>
      <template v-if="state.selectedRowKeys.length > 0">
        <a-popconfirm title="确定删除吗？" @confirm="handledeleteBatch">
          <a-button>
            <template #icon>
              <DeleteOutlined />
            </template>批量删除
          </a-button>
        </a-popconfirm>
        <a-button @click="handleCancelSelect">
          <template #icon>
            <UndoOutlined />
          </template>取消选择
        </a-button>
        <a-tag color="processing" style="float: right;">
          <template #icon>
            <CheckSquareOutlined />
          </template>
          {{ `已选择 ${state.selectedRowKeys.length} 条` }}
        </a-tag>
      </template>
    </div>
    <!--表格区域-->
    <a-table :dataSource="dataSource" :columns="columnsIndex" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange">
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'operation'">
          <a @click="handleGenerator(record)">
            <DatabaseOutlined />代码生成
          </a>
          <a @click="handleEdit(record)">
            <EditOutlined />编辑
          </a>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定删除吗？" @confirm="() => handleDelete(record.id)" placement="left">
            <a>
              <DeleteOutlined />删除
            </a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
    <!--弹窗区域-->
    <Operation ref="refOperation" :operationTitle="operationTitle" @childOK="loadData" />
  </a-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { useList } from '@/hooks/useList';

import Operation from './modal/Operation.vue';

import XCQueryForm from '@/components/xuanchen/XCQueryForm.vue';
import { GenTableApiUrl, generator } from './gentable.api';
import { queryParams, queryFormItems, columnsIndex } from './gentable.data';

/** url */
const url = reactive({
  list: GenTableApiUrl.INDEX_LIST,
  delete: GenTableApiUrl.INDEX_DELETE,
  deleteBatch: GenTableApiUrl.INDEX_DELETE_BATCH,
});

/** 重置 */
const handleReset = () => {
  queryParams.tableName = '';
  queryParams.tableComment = '';
  loadData();
}
/** 代码生成 */
const handleGenerator = async (record: any) => {
  await generator();
}

const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete, handledeleteBatch,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })
loadData();
</script>