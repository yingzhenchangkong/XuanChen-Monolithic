<template>
  <XCListPage :url="url" :columns="columnsIndex" :query-form-items="queryFormItemsIndex"
    :query-params="queryParamsIndex" :buttons="{ add: true, deleteBatch: true }">
    <!-- 优先级 / 状态列渲染为标签；其余列回退默认文本 -->
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'priority'">
        <a-tag :color="priorityColor(record.priority)">{{ priorityText(record.priority) }}</a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
      </template>
      <template v-else>{{ text }}</template>
    </template>

    <!-- 操作列：待办行 = 完成/编辑/删除；已完成行 = 删除 -->
    <template #operation="{ record, loadData, handleEdit, handleDelete }">
      <template v-if="record.status === 0">
        <a v-if="isBpmTask(record)" @click="handleBpmApprove(record, loadData)">
          <AuditOutlined /> 审批
        </a>
        <a-divider v-if="isBpmTask(record)" type="vertical"></a-divider>
        <a-popconfirm title="标记为已完成？" placement="left" @confirm="handleFinish(record, loadData)">
          <a>
            <CheckCircleOutlined /> 完成
          </a>
        </a-popconfirm>
        <a-divider type="vertical"></a-divider>
        <a @click="handleEdit(record)">
          <EditOutlined /> 编辑
        </a>
        <a-divider type="vertical"></a-divider>
      </template>
      <a-popconfirm title="确定删除吗？" placement="left" @confirm="handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
    </template>
  </XCListPage>

  <!-- 工作流审批弹窗（待办中心深度集成：审批任务待办直接拉起审批页） -->
  <BpmApprovalModal v-if="activeTaskId" :key="activeTaskId" ref="bpmApprovalRef" mode="approve"
    :task-id="activeTaskId" @handled="bpmHandled" />
</template>

<script lang="ts" setup>
import { nextTick, ref } from 'vue';
import { message } from 'ant-design-vue';
import { AuditOutlined, CheckCircleOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import BpmApprovalModal from '@/components/xuanchen/businessprocess/BpmApprovalModal.vue';
import Operation from './modal/Operation.vue';

import { queryParamsIndex, queryFormItemsIndex, columnsIndex, priorityColor, priorityText, statusColor, statusText } from './todo.data';
import { TodoApiUrl, finishTodoApi } from './todo.api';

/** 列表接口（list 走 GET 分页；delete/deleteBatch 走 DELETE，均按当前用户隔离） */
const url = {
  list: TodoApiUrl.INDEX_LIST,
  delete: TodoApiUrl.INDEX_DELETE,
  deleteBatch: TodoApiUrl.INDEX_DELETE_BATCH,
};

/** 标记完成 */
const handleFinish = async (record: Record<string, any>, loadData: () => void) => {
  const res = await finishTodoApi(String(record.id));
  if (res.code === 200) {
    message.success(res.msg);
    loadData();
  } else {
    message.error(res.msg);
  }
}

/** 工作流审批任务集成 */

/** 工作流任务的 content 约定："审批任务:<taskId>"（由 xuanchen-businessprocess 引擎写入） */
const WF_TODO_PREFIX = '审批任务:';
const activeTaskId = ref('');
const bpmApprovalRef = ref();

const isBpmTask = (record: Record<string, any>) =>
  String(record?.content ?? '').startsWith(WF_TODO_PREFIX);

/** 记录最近一次操作列注入的 loadData，审批完成后刷新列表 */
let listReload: (() => void) | null = null;

const handleBpmApprove = async (record: Record<string, any>, reload: () => void) => {
  listReload = reload;
  activeTaskId.value = String(record.content).slice(WF_TODO_PREFIX.length);
  await nextTick();
  bpmApprovalRef.value?.open();
};

/** 审批完成（同意/驳回/转办）后关闭对应待办并刷新列表 */
const bpmHandled = () => {
  activeTaskId.value = '';
  message.success('已处理');
  listReload?.();
};
</script>
