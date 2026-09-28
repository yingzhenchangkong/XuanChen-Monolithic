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
      <a-button @click="handleExport('用户管理')">
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
    <a-table :dataSource="dataSource" :columns="columns" :pagination="ipagination" :loading="loading"
      :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
      size="small" @change="handleTableChange">
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'operation'">
          <a @click="handleResetPassword(record.id)">
            <RetweetOutlined /> 重置密码
          </a>
          <a-divider type="vertical"></a-divider>
          <a @click="handleEdit(record)">
            <EditOutlined /> 编辑
          </a>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定删除吗？" @confirm="() => handleDelete(record.id)" placement="left">
            <a>
              <DeleteOutlined /> 删除
            </a>
          </a-popconfirm>
        </template>
        <template v-else-if="column.dataIndex === 'avatar'">
          <a-image :width="32" :height="32" :src="getImageView(record.avatar)"
            style="border-radius: 6px; object-fit: cover;" v-if="record.avatar"
            fallback="/images/avatar-fallback.png">
            <template #placeholder>
              <a-avatar class="avatar" shape="square">
                <template #icon>
                  <UserOutlined />
                </template>
              </a-avatar>
            </template>
          </a-image>
          <a-avatar class="avatar" shape="square" v-else>
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
            @click="handleStatusChange(record, index)">
            {{ dataSource[index].status === 1 ? '正常' : '冻结' }}
          </a-tag>
        </template>
      </template>
    </a-table>
    <!--弹窗区域-->
    <Operation ref="refOperation" :operationTitle="operationTitle" @childOK="loadData" />
    <RecycleBin ref="refRecycleBin" @childOK="loadData" />
    <ResetPassword ref="refResetPassword" />
  </a-card>
</template>

<script setup lang="ts">
import { useList } from '@/hooks/useList'
import type { UseListUrlConfig } from '@/hooks/useList'
import { ref } from 'vue';
import { getImageView } from '@/utils/ImageUtil';

import Operation from './modal/Operation.vue';
import RecycleBin from './modal/RecycleBin.vue';
import ResetPassword from './modal/ResetPassword.vue';

import XCQueryForm from '@/components/xuanchen/XCQueryForm.vue';
import { UserApiUrl, changeStatusApi } from './user.api';
import type { UserRecord } from './user.types';
import { queryParams, queryFormItems, columns } from './user.data';
import { message } from 'ant-design-vue';

/** url */
const url: UseListUrlConfig = {
  list: UserApiUrl.INDEX_LIST,
  delete: UserApiUrl.INDEX_DELETE,
  deleteBatch: UserApiUrl.INDEX_DELETE_BATCH,
  exportExcel: UserApiUrl.INDEX_EXPORT_EXCEL,
  importExcel: UserApiUrl.INDEX_IMPORT_EXCEL,
}

/** 重置 */
const handleReset = () => {
  queryParams.userName = ''
  queryParams.nickName = ''
  queryParams.mobile = ''
  loadData()
}

const handleStatusChange = async (record: UserRecord, index: number) => {
  const oldStatus = dataSource.value[index].status;
  const newStatus = record.status === 1 ? 2 : 1;
  // 乐观更新先翻 UI；业务失败或网络异常必须回滚，避免界面状态与数据库相反
  dataSource.value[index].status = newStatus;
  try {
    const res = await changeStatusApi(String(dataSource.value[index].id ?? ''), newStatus);
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

const refResetPassword = ref();
const handleResetPassword = (id: string) => {
  refResetPassword.value.show(id);
}

const refRecycleBin = ref();

/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show();
}
const {
  loadData,
  operationTitle, refOperation,
  handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList<UserRecord>({ url, queryParams })
loadData()
</script>