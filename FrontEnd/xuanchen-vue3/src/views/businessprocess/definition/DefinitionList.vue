<template>
  <XCListPage ref="listRef" :url="url" :columns="columns" :query-form-items="queryFormItems"
    :query-params="queryParams" :buttons="{ add: true }" export-title="流程配置">
    <!-- 操作列：设计 / 启用|停用 / 编辑 / 删除 -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="handleDesign(record)">
        <NodeIndexOutlined /> 设计
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm v-if="record.status === 1" title="确定停用该流程吗？" placement="left"
        @confirm="() => handleChangeStatus(record.id, 2)">
        <a>
          <PauseCircleOutlined /> 停用
        </a>
      </a-popconfirm>
      <a-popconfirm v-else title="确定启用该流程吗？启用前会校验流程图。" placement="left"
        @confirm="() => handleChangeStatus(record.id, 1)">
        <a>
          <CheckCircleOutlined /> 启用
        </a>
      </a-popconfirm>
      <a-divider type="vertical"></a-divider>
      <a @click="handleEdit(record)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="确定删除该流程定义吗？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 状态列 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="definitionStatusColor(record.status as number)">
          {{ definitionStatusText(record.status as number) }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'version'">
        v{{ record.version ?? 1 }}
      </template>
    </template>

    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  CheckCircleOutlined, DeleteOutlined, EditOutlined, NodeIndexOutlined, PauseCircleOutlined,
} from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import Operation from './modal/Operation.vue';
import { DefinitionApiUrl, changeDefinitionStatusApi } from './definition.api';
import { columns, definitionStatusColor, definitionStatusText, queryFormItems, queryParams } from '../businessprocess.data';

const router = useRouter();
const listRef = ref();

const url = reactive({
  list: DefinitionApiUrl.INDEX_LIST,
  delete: DefinitionApiUrl.INDEX_DELETE,
  deleteBatch: DefinitionApiUrl.INDEX_DELETE_BATCH,
});

const handleDesign = (record: Record<string, unknown>) => {
  const id = String(record.id ?? '');
  if (!id) return;
  router.push({ name: 'businessprocessDesigner', params: { id } });
};

const handleChangeStatus = async (id: string | undefined, status: number) => {
  if (!id) return;
  const res = await changeDefinitionStatusApi(id, status);
  if (res.code === 200) {
    message.success(status === 1 ? '启用成功！' : '停用成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '操作失败');
  }
};
</script>
