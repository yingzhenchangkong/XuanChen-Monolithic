<template>
  <a-card class="bpm-task-page">
    <div class="bpm-task-toolbar">
      <a-input-search v-model:value="keyword" placeholder="搜索摘要 / 节点名称" allowClear style="width: 280px"
        @search="handleSearch" />
      <a-button @click="loadData">
        <ReloadOutlined /> 刷新
      </a-button>
      <a-button class="bpm-delegate-btn" @click="delegateRef?.open()">
        <UserSwitchOutlined /> 委托设置
      </a-button>
    </div>

    <a-table :columns="todoColumns" :data-source="rows" :loading="loading" row-key="id"
      :pagination="pagination" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'nodeName'">
          {{ record.nodeName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a @click="handleApprove(record as BpmTaskRecord)">
            <AuditOutlined /> 审批
          </a>
        </template>
      </template>
    </a-table>

    <BpmApprovalModal v-if="activeTask" :key="activeTask.id" ref="approvalRef" mode="approve"
      :task-id="activeTask.id" :task-node-name="activeTask.nodeName" @handled="loadData" />

    <BpmDelegateModal ref="delegateRef" />
  </a-card>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { AuditOutlined, ReloadOutlined, UserSwitchOutlined } from '@ant-design/icons-vue';

import BpmApprovalModal from '@/components/xuanchen/businessprocess/BpmApprovalModal.vue';
import BpmDelegateModal from '@/components/xuanchen/businessprocess/BpmDelegateModal.vue';
import { getTodoTasksApi } from '../businessprocess.api';
import type { BpmTaskRecord } from '../businessprocess.types';

const loading = ref(false);
const keyword = ref('');
const rows = ref<BpmTaskRecord[]>([]);
const activeTask = ref<BpmTaskRecord | null>(null);
const approvalRef = ref();
const delegateRef = ref();

const todoColumns = [
  { title: '#', key: 'rowIndex', width: 60, customRender: ({ index }: { index: number }) => index + 1 },
  { title: '申请摘要', dataIndex: 'businessSummary', align: 'left', ellipsis: true },
  { title: '当前节点', dataIndex: 'nodeName', align: 'left', width: 160 },
  { title: '到达时间', dataIndex: 'createTime', align: 'center', width: 170 },
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
    const res = await getTodoTasksApi({
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

const handleApprove = async (record: BpmTaskRecord) => {
  activeTask.value = record;
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

.bpm-delegate-btn {
  margin-left: auto;
}
</style>
