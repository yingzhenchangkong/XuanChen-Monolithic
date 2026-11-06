<template>
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ add: true, import: true, export: true, recycleBin: true, deleteBatch: true }" export-title="角色管理"
    :status-api="changeStatusApi" @recycle-bin="handleRecycleBin">
    <!-- 操作列：用户 / 授权 / 编辑 / 删除 -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="handleAssignUser(record.id)">
        <UserAddOutlined />账号
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleAssignMenu(record.id)">
        <MenuOutlined />授权
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
      <AssignMenu ref="refAssignMenu" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import RecycleBin from '@/components/xuanchen/XCRecycleBin.vue';
import Operation from './modal/Operation.vue';
import AssignUser from './modal/AssignUser.vue';
import AssignMenu from './modal/AssignMenu.vue';

import { queryParams, queryFormItems, columnsIndex, columnsRecBin } from './role.data';
import { RoleApiUrl, changeStatusApi } from './role.api';

/** 列表接口 */
const url = reactive({
  list: RoleApiUrl.INDEX_LIST,
  delete: RoleApiUrl.INDEX_DELETE,
  deleteBatch: RoleApiUrl.INDEX_DELETE_BATCH,
  exportExcel: RoleApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: RoleApiUrl.INDEX_IMPORT_EXCEL,
});

/** 回收站接口 */
const recycleUrl = {
  list: RoleApiUrl.RECBIN_LIST,
  delete: RoleApiUrl.RECBIN_DELETE,
  deleteBatch: RoleApiUrl.RECBIN_DELETE_BATCH,
  revert: RoleApiUrl.RECBIN_REVERT,
  revertBatch: RoleApiUrl.RECBIN_REVERT_BATCH,
};

const refAssignUser = ref();
const refAssignMenu = ref();
const refRecycleBin = ref();

/** 分配用户 */
const handleAssignUser = (roleId: string) => {
  refAssignUser.value.show(roleId);
}
/** 分配菜单 */
const handleAssignMenu = (roleId: string) => {
  refAssignMenu.value.show(roleId);
}
/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show();
}
</script>
