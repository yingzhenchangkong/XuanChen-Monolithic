<template>
  <a-card class="bpm-task-page">
    <div class="bpm-task-toolbar">
      <a-input-search v-model:value="keyword" placeholder="搜索摘要 / 流程名称" allowClear style="width: 280px"
        @search="handleSearch" />
      <a-select v-model:value="statusFilter" placeholder="状态" allowClear style="width: 130px"
        :options="instanceStatusOptions" @change="handleSearch" />
      <a-button @click="loadData">
        <ReloadOutlined /> 刷新
      </a-button>
    </div>

    <a-table :columns="mineColumns" :data-source="rows" :loading="loading" row-key="id"
      :pagination="pagination" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="instanceStatusColor(record.status)">
            {{ instanceStatusText(record.status) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'finishTime'">
          {{ record.finishTime || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a @click="handleView(record as BpmInstanceRecord)">
            <EyeOutlined /> 详情
          </a>
          <template v-if="Number(record.status) === 1">
            <a-divider type="vertical" />
            <a-popconfirm title="确定催办吗？将向当前审批人发送提醒。" placement="left"
              @confirm="() => handleUrge(record as BpmInstanceRecord)">
              <a>
                <NotificationOutlined /> 催办
              </a>
            </a-popconfirm>
            <a-divider type="vertical" />
            <a-popconfirm title="确定撤回该申请吗？仅当前节点无人处理时可撤回。"
              @confirm="() => handleRevoke(record as BpmInstanceRecord)">
              <a>
                <RollbackOutlined /> 撤回
              </a>
            </a-popconfirm>
          </template>
        </template>
      </template>
    </a-table>

    <BpmApprovalModal v-if="activeInstanceId" :key="activeInstanceId" ref="approvalRef" mode="view" />
  </a-card>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { EyeOutlined, NotificationOutlined, ReloadOutlined, RollbackOutlined } from '@ant-design/icons-vue';

import BpmApprovalModal from '@/components/xuanchen/businessprocess/BpmApprovalModal.vue';
import { getMyInstancesApi, revokeInstanceApi, urgeInstanceApi } from '../businessprocess.api';
import { instanceStatusColor, instanceStatusOptions, instanceStatusText } from '../businessprocess.data';
import type { BpmInstanceRecord } from '../businessprocess.types';

const loading = ref(false);
const keyword = ref('');
const statusFilter = ref<number | undefined>(undefined);
const rows = ref<BpmInstanceRecord[]>([]);
const activeInstanceId = ref('');
const approvalRef = ref();

const mineColumns = [
  { title: '#', key: 'rowIndex', width: 60, customRender: ({ index }: { index: number }) => index + 1 },
  { title: '申请摘要', dataIndex: 'businessSummary', align: 'left', ellipsis: true },
  { title: '流程名称', dataIndex: 'definitionName', align: 'left', width: 160 },
  { title: '状态', dataIndex: 'status', align: 'center', width: 100 },
  { title: '发起时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '办结时间', dataIndex: 'finishTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 200 },
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
    const res = await getMyInstancesApi({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      status: statusFilter.value,
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

const handleView = async (record: BpmInstanceRecord) => {
  activeInstanceId.value = record.id;
  await nextTick();
  approvalRef.value?.open(record, record.id);
};

const handleRevoke = async (record: BpmInstanceRecord) => {
  const res = await revokeInstanceApi(record.id);
  if (res.code === 200) {
    message.success('撤回成功！');
    loadData();
  } else {
    message.error(res.msg || '撤回失败');
  }
};

const handleUrge = async (record: BpmInstanceRecord) => {
  const res = await urgeInstanceApi(record.id);
  if (res.code === 200) {
    message.success(res.msg || '已催办！');
  } else {
    message.error(res.msg || '催办失败');
  }
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
