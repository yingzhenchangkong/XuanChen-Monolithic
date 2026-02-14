<template>
  <a-card>
    <!-- 查询区域 -->
    <QueryFormXC v-model="queryParams" :formItems="queryFormItems" @search="handleSearch" @reset="handleReset" />
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
import { queryParams, queryFormItems, columns } from './logoperation.data';
import { LogOperationApiUrl } from './logoperation.api';
import { getUserSelect } from '@/views/system/user/user.api';
import { getDictSelect } from '@/views/system/dict/dict.api';

/** url */
const url = reactive({
  list: LogOperationApiUrl.LIST,
  exportExcel: LogOperationApiUrl.EXPORT_EXCEL,
})
const handleSearch = () => {
  if (queryParams.timeRange.length > 0) {
    queryParams.beginTime = queryParams.timeRange[0];
    queryParams.endTime = queryParams.timeRange[1];
  }
  const temp = queryParams.timeRange;
  queryParams.timeRange = [];
  loadData();
  queryParams.timeRange = temp;
}
/** 重置 */
const handleReset = () => {
  queryParams.userName = undefined;
  queryParams.timeRange = [];
  queryParams.beginTime = undefined;
  queryParams.endTime = undefined;
  queryParams.status = undefined;
  loadData();
}
const {
  loadData,
  handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange
} = useList({ url, queryParams })
loadData();

const loadUserSelect = async () => {
  queryFormItems[0]!.options = await getUserSelect();
};
loadUserSelect();
const loadStatus = async () => {
  queryFormItems[2]!.options = await getDictSelect("succ_fail");
};
loadStatus();
</script>