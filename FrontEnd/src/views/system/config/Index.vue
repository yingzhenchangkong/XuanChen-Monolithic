<template>
  <a-card>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParams" :formItems="queryFormItems" @search="loadData" @reset="handleReset" />
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button type="primary" @click="handleAdd">
        <template #icon>
          <PlusOutlined />
        </template>新增
      </a-button>
      <a-upload name="file" :customRequest="handleImport" :showUploadList="false">
        <a-button>
          <template #icon>
            <UploadOutlined />
          </template>导入
        </a-button>
      </a-upload>
      <a-button @click="handleExport('配置管理')">
        <template #icon>
          <DownloadOutlined />
        </template>导出
      </a-button>
      <a-button @click="handleRecycleBin">
        <template #icon>
          <RestOutlined />
        </template>回收站
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
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === true ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
            @click="handleStatusChange(record, index)">
            {{ dataSource[index].status === true ? '启用' : '停用' }}
          </a-tag>
        </template>
      </template>
    </a-table>
    <!--弹窗区域-->
    <Operation ref="refOperation" :operationTitle="operationTitle" @childOK="loadData" />
    <RecycleBin ref="refRecycleBin" @childOK="loadData" />
  </a-card>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useList } from '@/hooks/useList';

import Operation from './modal/Operation.vue';
import RecycleBin from './modal/RecycleBin.vue';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { ConfigApiUrl, changeStatusApi } from './config.api';
import { queryParams, queryFormItems, columnsIndex } from './config.data';

/** url */
const url = reactive({
  list: ConfigApiUrl.INDEX_LIST,
  delete: ConfigApiUrl.INDEX_DELETE,
  deleteBatch: ConfigApiUrl.INDEX_DELETE_BATCH,
  exportExcel: ConfigApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: ConfigApiUrl.INDEX_IMPORT_EXCEL,
})

/** 重置 */
const handleReset = () => {
  queryParams.configName = ''
  queryParams.configDescription = ''
  loadData();
}

/** 回收站 */
const refRecycleBin = ref();
const handleRecycleBin = () => {
  refRecycleBin.value.show();
}

const handleStatusChange = async (record: any, index: number) => {
  dataSource.value[index].status = record.status === true ? false : true;
  const res: any = await changeStatusApi(dataSource.value[index].id, dataSource.value[index].status);
  if (res.code === 200) {
    message.success(res.msg);
  } else {
    message.error(res.msg);
  }
}

const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })
loadData()
</script>