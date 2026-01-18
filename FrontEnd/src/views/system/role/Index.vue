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
      <a-button @click="handleExport('角色管理')">
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
          <a @click="handleAssignUser(record.id)">
            <UserAddOutlined />分配用户
          </a>
          <a-divider type="vertical"></a-divider>
          <a @click="handleAssignMenu(record.id)">
            <MenuOutlined />分配菜单
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
    <AssignUser ref="refAssignUser" />
    <AssignMenu ref="refAssignMenu" />
  </a-card>
</template>

<script setup lang="ts">
import { useList } from '@/hooks/useList'
import { reactive, ref } from 'vue';
import Operation from './modal/Operation.vue';
import RecycleBin from './modal/RecycleBin.vue';
import AssignUser from './modal/AssignUser.vue';
import AssignMenu from './modal/AssignMenu.vue';

import QueryFormXC from '@/components/xuanchen/QueryFormXC.vue';
import { RoleApiUrl } from './role.api';
import { queryParams, queryFormItems, columnsIndex } from './role.data';

/** url */
const url = reactive({
  list: RoleApiUrl.INDEX_LIST,
  delete: RoleApiUrl.INDEX_DELETE,
  deleteBatch: RoleApiUrl.INDEX_DELETE_BATCH,
  exportExcel: RoleApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: RoleApiUrl.INDEX_IMPORT_EXCEL,
})

/** 重置 */
const handleReset = () => {
  queryParams.roleName = ''
  queryParams.roleDescription = ''
  loadData();
}

const refAssignUser = ref();
const refAssignMenu = ref();
const refRecycleBin = ref();

/** 分配用户 */
const handleAssignUser = (roleId: string) => {
  refAssignUser.value.show(roleId)
}
/** 分配菜单 */
const handleAssignMenu = (roleId: string) => {
  refAssignMenu.value.show(roleId)
}
/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show();
}

const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })
loadData()
</script>