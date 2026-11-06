<template>
  <a-card class="bpm-task-page">
    <div class="bpm-task-toolbar">
      <a-input-search v-model:value="keyword" placeholder="搜索摘要 / 节点名称" allowClear style="width: 280px"
        @search="handleSearch" />
      <a-button @click="loadData">
        <ReloadOutlined /> 刷新
      </a-button>
    </div>

    <a-table :columns="doneColumns" :data-source="rows" :loading="loading" row-key="id"
      :pagination="pagination" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'result'">
          <a-tag :color="Number(record.result) === 1 ? 'green' : 'red'">
            {{ Number(record.result) === 1 ? '同意' : '驳回' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'opinion'">
          <span :title="record.opinion">{{ record.opinion || '-' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'handleTime'">
          {{ record.handleTime || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a @click="handleView(record as BpmTaskRecord)">
            <EyeOutlined /> 查看
          </a>
        </template>
      </template>
    </a-table>

    <BpmApprovalModal v-if="activeInstanceId" :key="activeInstanceId" ref="approvalRef" mode="view" />
  </a-card>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons-vue';

import BpmApprovalModal from '@/components/xuanchen/businessprocess/BpmApprovalModal.vue';
import { getDoneTasksApi } from '../businessprocess.api';
import type { BpmTaskRecord } from '../businessprocess.types';

const loading = ref(false);
const keyword = ref('');
const rows = ref<BpmTaskRecord[]>([]);
const activeInstanceId = ref('');
const approvalRef = ref();

const doneColumns = [
  { title: '#', key: 'rowIndex', width: 60, customRender: ({ index }: { index: number }) => index + 1 },
  { title: '申请摘要', dataIndex: 'businessSummary', align: 'left', ellipsis: true },
  { title: '审批节点', dataIndex: 'nodeName', align: 'left', width: 140 },
  { title: '结果', dataIndex: 'result', align: 'center', width: 90 },
  { title: '意见', dataIndex: 'opinion', align: 'left', ellipsis: true },
  { title: '处理时间', dataIndex: 'handleTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 100 },
];

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getDoneTasksApi({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      businessSummary: keyword.value || undefined,
    });
    if (res.code === 200) {
      rows.value = res.data?.records ?? [];
      pagination.total = res.data?.total ?? 0;
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.current = 1;
  loadData();
};

const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
};

const handleView = async (record: BpmTaskRecord) => {
  activeInstanceId.value = record.instanceId;
  await nextTick();
  approvalRef.value?.open({ id: record.instanceId } as any, record.instanceId);
};

onMounted(loadData);
</script>

<style scoped>
.bpm-task-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
