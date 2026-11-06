<template>
  <XCListPage :url="url" :columns="columns" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ add: true, import: true, export: true, recycleBin: true, deleteBatch: true }" export-title="账号管理"
    :status-api="changeStatusApi" :status-compute="statusCompute" @recycle-bin="handleRecycleBin">
    <!-- 操作列：重置密码 / 编辑 / 删除 -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="handleResetPassword(record.id)">
        <RetweetOutlined /> 重置密码
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleEdit(record)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="确定删除吗？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 头像列（点击图片可预览）/ 账号状态列 -->
    <template #bodyCell="{ column, record, index, dataSource, handleStatusChange }">
      <template v-if="column.dataIndex === 'avatar'">
        <a-image v-if="record.avatar" :src="getImageView(record.avatar) ?? undefined" :width="40" :height="40"
          :style="{ objectFit: 'cover', borderRadius: '4px' }" />
        <span v-else>-</span>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="record.status === 1 ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
          @click="handleStatusChange(record, index)">
          {{ dataSource[index].status === 1 ? '正常' : '冻结' }}
        </a-tag>
      </template>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
      <RecycleBin ref="refRecycleBin" :url="recycleUrl" :columns="columnsRcvBin"
        :query-form-items="queryFormItemsRcvBin" :query-params="recycleQueryParams" primary-buttons
        @childOK="loadData" />
      <ResetPassword ref="refResetPassword" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import RecycleBin from '@/components/xuanchen/XCRecycleBin.vue';
import Operation from './modal/Operation.vue';
import ResetPassword from './modal/ResetPassword.vue';
import { getImageView } from '@/utils/ImageUtil';

import { AccountApiUrl, changeStatusApi } from './account.api';
import { queryParams, queryFormItems, columns, queryFormItemsRcvBin, columnsRcvBin } from './account.data';

/** 列表接口 */
const url = reactive({
  list: AccountApiUrl.INDEX_LIST,
  delete: AccountApiUrl.INDEX_DELETE,
  deleteBatch: AccountApiUrl.INDEX_DELETE_BATCH,
  exportExcel: AccountApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: AccountApiUrl.INDEX_IMPORT_EXCEL,
});

/** 回收站接口 */
const recycleUrl = {
  list: AccountApiUrl.REC_BIN_LIST,
  delete: AccountApiUrl.REC_BIN_DELETE,
  deleteBatch: AccountApiUrl.REC_BIN_DELETE_BATCH,
  revert: AccountApiUrl.REC_BIN_REVERT,
  revertBatch: AccountApiUrl.REC_BIN_REVERT_BATCH,
};

/** 回收站查询条件默认值（仅用户名） */
const recycleQueryParams = { userName: '' };

/** 账号状态换算：1 正常 ↔ 2 冻结（与其它模块的 1 ↔ 0 不同） */
const statusCompute = (current: number) => (current === 1 ? 2 : 1);

const refRecycleBin = ref();
/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show();
}

/** 重置密码 */
const refResetPassword = ref();
const handleResetPassword = (id: string) => {
  refResetPassword.value.show(id);
}
</script>
