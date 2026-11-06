<template>
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ add: true, import: true, export: true, recycleBin: true, deleteBatch: true }" export-title="配置管理"
    :status-api="changeStatusApi" @recycle-bin="handleRecycleBin">
    <!-- 状态列（操作列走组件默认的「编辑 / 删除」） -->
    <template #bodyCell="{ column, record, index, dataSource, handleStatusChange }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="record.status === 1 ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
          @click="handleStatusChange(record, index)">
          {{ dataSource[index].status === 1 ? '启用' : '停用' }}
        </a-tag>
      </template>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
      <RecycleBin ref="refRecycleBin" :url="recycleUrl" :columns="columnsRecBin" :query-form-items="queryFormItems"
        :query-params="queryParams" @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import RecycleBin from '@/components/xuanchen/XCRecycleBin.vue';
import Operation from './modal/Operation.vue';

import { ConfigApiUrl, changeStatusApi } from './config.api';
import { queryParams, queryFormItems, columnsIndex, columnsRecBin } from './config.data';

/** 列表接口 */
const url = reactive({
  list: ConfigApiUrl.INDEX_LIST,
  delete: ConfigApiUrl.INDEX_DELETE,
  deleteBatch: ConfigApiUrl.INDEX_DELETE_BATCH,
  exportExcel: ConfigApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: ConfigApiUrl.INDEX_IMPORT_EXCEL,
});

/** 回收站接口 */
const recycleUrl = {
  list: ConfigApiUrl.RECBIN_LIST,
  delete: ConfigApiUrl.RECBIN_DELETE,
  deleteBatch: ConfigApiUrl.RECBIN_DELETE_BATCH,
  revert: ConfigApiUrl.RECBIN_REVERT,
  revertBatch: ConfigApiUrl.RECBIN_REVERT_BATCH,
};

const refRecycleBin = ref();
/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show();
}
</script>
