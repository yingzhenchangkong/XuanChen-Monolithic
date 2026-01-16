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
      <a-button @click="handleExport('岗位管理')">
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
      <template #bodyCell="{ column, text, record }">
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
          <a-tag :color="record.status === true ? 'green' : 'volcano'">
            {{ record.status === true ? '启用' : '停用' }}
          </a-tag>
        </template>
      </template>
    </a-table>
    <!--弹窗区域-->
    <Operation ref="refOperation" :operationTitle="operationTitle" @childOK="loadData" />
    <RecycleBin ref="refRecycleBin" @childOK="loadData" />
  </a-card>
</template>

<script lang="ts" setup>
import { useList } from '@/hooks/useList'
import { reactive, ref } from 'vue';
import Operation from './modal/Operation.vue';
import RecycleBin from './modal/RecycleBin.vue';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { queryParams, queryFormItems, columnsIndex } from './post.data';
import { PostApiUrl } from './post.api';

/** url */
const url = reactive({
  list: PostApiUrl.INDEX_LIST,
  delete: PostApiUrl.INDEX_DELETE,
  deleteBatch: PostApiUrl.INDEX_DELETE_BATCH,
  exportExcel: PostApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: PostApiUrl.INDEX_IMPORT_EXCEL,
})

/** 重置 */
const handleReset = () => {
  queryParams.postName = ''
  queryParams.postDescription = ''
  loadData()
}

const refRecycleBin = ref();
/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show()
}

const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })
loadData()
</script>