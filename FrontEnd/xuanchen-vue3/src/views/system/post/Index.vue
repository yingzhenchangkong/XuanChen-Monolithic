<template>
  <a-card>
    <!-- 查询区域 -->
    <XCQueryForm v-model="queryParams" :formItems="queryFormItems" @search="loadData" @reset="handleReset" />
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
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'operation'">
          <a @click="handleAssignUser(record.id)">
            <UserAddOutlined />员工
          </a>
          <a-divider type="vertical"></a-divider>
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
          <a-tag :color="record.status === 1 ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
            @click="handleStatusChange(record, index)">
            {{ dataSource[index].status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
      </template>
    </a-table>
    <!--弹窗区域-->
    <Operation ref="refOperation" :operationTitle="operationTitle" @childOK="loadData" />
    <RecycleBin ref="refRecycleBin" @childOK="loadData" />
    <AssignUser ref="refAssignUser" />
  </a-card>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useList } from '@/hooks/useList';

import Operation from './modal/Operation.vue';
import RecycleBin from './modal/RecycleBin.vue';
import AssignUser from './modal/AssignUser.vue';

import XCQueryForm from '@/components/xuanchen/XCQueryForm.vue';
import { queryParams, queryFormItems, columnsIndex } from './post.data';
import { PostApiUrl, changeStatusApi } from './post.api';
import type { PostRecord } from './post.types';

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

const handleStatusChange = async (record: any, index: number) => {
  const oldStatus = dataSource.value[index].status;
  const newStatus = record.status === 1 ? 0 : 1;
  // 乐观更新先翻 UI；业务失败或网络异常必须回滚，避免界面状态与数据库相反
  dataSource.value[index].status = newStatus;
  try {
    const res = await changeStatusApi(dataSource.value[index].id, newStatus);
    if (res.code === 200) {
      message.success(res.msg);
    } else {
      dataSource.value[index].status = oldStatus;
      message.error(res.msg);
    }
  } catch {
    dataSource.value[index].status = oldStatus;
    // HTTP/网络错误已由响应拦截器统一提示
  }
}

/** 分配员工 */
const refAssignUser = ref();
const handleAssignUser = (postId: string) => {
  refAssignUser.value.show(postId);
}

const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList<PostRecord>({ url, queryParams })
loadData()
</script>