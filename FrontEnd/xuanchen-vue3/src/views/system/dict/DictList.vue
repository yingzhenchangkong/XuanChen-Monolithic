<template>
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItemsIndex"
    :query-params="queryParamsIndex" :buttons="{ add: true }" selection="radio" :default-page-size="5"
    :status-api="changeStatusApi" @selection-change="handleSelectionChange" @search="handleSearch" @reset="handleReset">
    <!-- 操作列：编辑 / 删除（删除后清空下方明细的选中项） -->
    <template #operation="{ record, handleEdit, loadData }">
      <a @click="handleEdit(record)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="确定删除吗？" placement="left" @confirm="() => handleDelete(record.id, loadData)">
        <a>
          <DeleteOutlined /> 删除
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

    <!-- 表格下方：当前选中字典的明细 -->
    <template #footer>
      <a-tabs defaultActiveKey="1">
        <a-tab-pane tab="字典明细" key="1">
          <DictItemList :mainId="mainId" />
        </a-tab-pane>
      </a-tabs>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <OperationIndex :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { message } from 'ant-design-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import OperationIndex from './modal/OperationIndex.vue';
import DictItemList from './DictItemList.vue';

import { DictApiUrl, deleteDictApi, changeStatusApi } from './dict.api';
import { queryParamsIndex, queryFormItemsIndex, columnsIndex } from './dict.data';

/** 列表接口 */
const url = {
  list: DictApiUrl.INDEX_LIST,
}

/** 当前选中的字典编码（驱动下方明细） */
const mainId = ref('');

/** 单选变化时同步选中字典 */
const handleSelectionChange = (keys: (string | number)[], rows: Record<string, unknown>[]) => {
  mainId.value = keys.length > 0 ? String(rows[0]?.dictCode ?? '') : '';
}
/** 搜索：清空明细选中项 */
const handleSearch = () => {
  mainId.value = '';
}
/** 重置：清空明细选中项 */
const handleReset = () => {
  mainId.value = '';
}
/** 删除字典 */
const handleDelete = async (id: string, loadData: () => void) => {
  const res = await deleteDictApi(id);
  if (res.code == 200) {
    message.success(res.msg);
    mainId.value = '';
    loadData();
  } else {
    message.warning(res.msg);
  }
}
</script>
