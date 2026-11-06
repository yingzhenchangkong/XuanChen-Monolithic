<template>
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItems" :query-params="queryParams"
    :buttons="{ add: true, recycleBin: true, deleteBatch: true }" :status-api="changeStatusApi"
    @recycle-bin="handleRecycleBin">
    <!-- 状态列：点击切换启停（组件内置 useStatusToggle，乐观更新 + 失败回滚） -->
    <template #bodyCell="{ column, record, index, dataSource, handleStatusChange }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="record.status === 1 ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
          @click="handleStatusChange(record, index)">
          {{ dataSource[index].status === 1 ? '启用' : '停用' }}
        </a-tag>
      </template>
    </template>

    <!-- 操作列：文档管理 + 默认编辑/删除 -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="openDocs(record)">
        <FolderOpenOutlined /> 文档
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleEdit(record)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="删除知识库会同时清理其下文档向量，确定删除吗？" placement="left"
        @confirm="handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
      <RecycleBin ref="refRecycleBin" :url="recycleUrl" :columns="columnsRecBin" :query-form-items="queryFormItems"
        :query-params="queryParams" @childOK="loadData" />
    </template>
  </XCListPage>

  <!-- 文档管理抽屉 -->
  <DocList v-model:open="docsOpen" :kb="activeKb" />
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import RecycleBin from '@/components/xuanchen/XCRecycleBin.vue';
import Operation from './modal/Operation.vue';
import DocList from './DocList.vue';

import { AiKbUrl, changeStatusApi } from './knowledge.api';
import { queryParams, queryFormItems, columnsIndex, columnsRecBin } from './knowledge.data';
import type { AiKbRecord } from './knowledge.types';

/** 列表接口 */
const url = reactive({
  list: AiKbUrl.INDEX_LIST,
  delete: AiKbUrl.OPERATION_DELETE,
  deleteBatch: AiKbUrl.OPERATION_DELETE_BATCH,
});

/** 回收站接口 */
const recycleUrl = {
  list: AiKbUrl.RECBIN_LIST,
  delete: AiKbUrl.RECBIN_DELETE,
  deleteBatch: AiKbUrl.RECBIN_DELETE_BATCH,
  revert: AiKbUrl.RECBIN_REVERT,
  revertBatch: AiKbUrl.RECBIN_REVERT_BATCH,
};

const refRecycleBin = ref();
/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value.show();
};

/** 文档管理抽屉 */
const docsOpen = ref(false);
const activeKb = ref<AiKbRecord | null>(null);
const openDocs = (record: Record<string, any>) => {
  activeKb.value = record as AiKbRecord;
  docsOpen.value = true;
};
</script>
