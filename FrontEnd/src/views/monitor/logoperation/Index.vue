<template>
  <a-card>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParams" :formItems="queryFormItems" @search="loadData" @reset="handleReset" />
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button @click="handleExport('操作日志')">
        <template #icon>
          <DownloadOutlined />
        </template>导出
      </a-button>
    </div>
    <!--表格区域-->
    <a-table :dataSource="dataSource" :columns="columns" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange" :scroll="{ x: 'max-content' }">
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === true ? 'green' : 'volcano'">
            {{ record.status === true ? '操作成功' : '操作失败' }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </a-card>
</template>
<script setup lang="ts">
import { reactive } from 'vue';
import { useList } from '@/hooks/useList';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { LogOperationApiUrl } from './logoperation.api';
import { queryParams, queryFormItems, columns } from './logoperation.data';

/** url */
const url = reactive({
  list: LogOperationApiUrl.LIST,
  exportExcel: LogOperationApiUrl.EXPORT_EXCEL,
})

/** 重置 */
const handleReset = () => {
  queryParams.userName = '';
  queryParams.loginTimeStart = '';
  queryParams.loginTimeEnd = '';
  queryParams.status = '';
  loadData();
}
const {
  loadData,
  handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange
} = useList({ url, queryParams })
loadData()
</script>