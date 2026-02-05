<template>
  <a-card>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParams" :formItems="queryFormItems" @search="loadData" @reset="handleReset" />
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button @click="handleExport('登录日志')">
        <template #icon>
          <DownloadOutlined />
        </template>导出
      </a-button>
    </div>
    <!--表格区域-->
    <a-table :dataSource="dataSource" :columns="columns" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange">
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === true ? 'green' : 'volcano'">
            {{ record.status === true ? '登录成功' : '登录失败' }}
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
import { LogLoginApiUrl } from './loglogin.api';
import { queryParams, queryFormItems, columns } from './loglogin.data';

/** url */
const url = reactive({
  list: LogLoginApiUrl.LIST,
  exportExcel: LogLoginApiUrl.EXPORT_EXCEL,
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