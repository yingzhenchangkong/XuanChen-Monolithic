<template>
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ add: true, import: true, export: true, recycleBin: true, deleteBatch: true }" export-title="岗位管理"
    :status-api="changeStatusApi" @recycle-bin="handleRecycleBin">
    <!-- 操作列：人员 / 编辑 / 删除 -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="handleAssignUser(record.id)">
        <UserAddOutlined />人员
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleEdit(record)">
        <EditOutlined />编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="确定删除吗？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined />删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 状态列 -->
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
      <AssignUser ref="refAssignUser" />
    </template>
  </XCListPage>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import RecycleBin from '@/components/xuanchen/XCRecycleBin.vue';
import Operation from './modal/Operation.vue';
import AssignUser from './modal/AssignUser.vue';

import { queryParams, queryFormItems, columnsIndex, columnsRecBin } from './post.data';
import { PostApiUrl, changeStatusApi } from './post.api';

/** 列表接口 */
const url = reactive({
  list: PostApiUrl.INDEX_LIST,
  delete: PostApiUrl.INDEX_DELETE,
  deleteBatch: PostApiUrl.INDEX_DELETE_BATCH,
  exportExcel: PostApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: PostApiUrl.INDEX_IMPORT_EXCEL,
});

/** 回收站接口 */
const recycleUrl = {
  list: PostApiUrl.RECBIN_LIST,
  delete: PostApiUrl.RECBIN_DELETE,
  deleteBatch: PostApiUrl.RECBIN_DELETE_BATCH,
  revert: PostApiUrl.RECBIN_REVERT,
  revertBatch: PostApiUrl.RECBIN_REVERT_BATCH,
};

const refRecycleBin = ref();
/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show()
}

/** 分配人员 */
const refAssignUser = ref();
const handleAssignUser = (postId: string) => {
  refAssignUser.value.show(postId);
}
</script>
