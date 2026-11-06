<template>
  <a-modal v-model:open="visible" title="审批委托设置" :footer="null" :width="680">
    <a-alert class="bpm-delegate-tip" type="info" show-icon
      message="启用中的委托规则在生效期内，派给你的审批任务会同步派给被委托人（双方均可处理）。" />

    <!-- 新增规则 -->
    <a-form layout="inline" class="bpm-delegate-form">
      <a-form-item label="被委托人">
        <a-select v-model:value="form.delegateUserName" show-search option-filter-prop="label"
          :options="userOptions" placeholder="请选择" style="width: 180px" />
      </a-form-item>
      <a-form-item label="生效期">
        <a-range-picker v-model:value="form.range" show-time format="YYYY-MM-DD HH:mm"
          :placeholder="['开始时间', '结束时间']" style="width: 320px" />
      </a-form-item>
      <a-form-item label="备注">
        <a-input v-model:value="form.remark" :maxlength="100" placeholder="如：请假" style="width: 120px" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="adding" @click="handleAdd">添加</a-button>
      </a-form-item>
    </a-form>

    <!-- 规则列表 -->
    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="small"
      :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'delegateUserName'">
          {{ nickOf(record.delegateUserName) }}
        </template>
        <template v-else-if="column.dataIndex === 'range'">
          {{ (record.startTime || '立即') + ' ~ ' + (record.endTime || '长期') }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a @click="handleToggleStatus(record as BpmDelegateRecord)">
            {{ record.status === 1 ? '停用' : '启用' }}
          </a>
          <a-divider type="vertical" />
          <a-popconfirm title="确定删除该委托规则吗？" @confirm="handleDelete(record as BpmDelegateRecord)">
            <a class="bpm-danger-link">删除</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';

import {
  addDelegateApi, changeDelegateStatusApi, deleteDelegateApi, getDelegateListApi, getUserOptionsApi,
} from '@/views/businessprocess/businessprocess.api';
import type { BpmUserOption } from '@/views/businessprocess/businessprocess.types';
import type { BpmDelegateRecord } from '@/views/businessprocess/businessprocess.types';

const visible = ref(false);
const loading = ref(false);
const adding = ref(false);
const rows = ref<BpmDelegateRecord[]>([]);
const userOptions = ref<Array<{ label: string; value: string }>>([]);
const nickMap = ref<Record<string, string>>({});
const form = ref<{
  delegateUserName?: string;
  range?: [Dayjs, Dayjs] | null;
  remark?: string;
}>({});

const columns = [
  { title: '被委托人', dataIndex: 'delegateUserName', align: 'left', width: 140 },
  { title: '生效期', dataIndex: 'range', align: 'left' },
  { title: '状态', dataIndex: 'status', align: 'center', width: 80 },
  { title: '备注', dataIndex: 'remark', align: 'left', width: 110, ellipsis: true },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 110 },
];

const nickOf = (userName?: string) =>
  (userName && nickMap.value[userName]) || userName || '-';

const loadUsers = async () => {
  if (userOptions.value.length) return;
  try {
    const users = await getUserOptionsApi();
    userOptions.value = users.map((u: BpmUserOption) => ({
      label: `${u.nickName}（${u.userName}）`, value: u.userName,
    }));
    const map: Record<string, string> = {};
    users.forEach((u: BpmUserOption) => {
      map[u.userName] = u.nickName;
    });
    nickMap.value = map;
  } catch {
    /* 下拉失败不阻塞列表 */
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getDelegateListApi();
    if (res.code === 200) {
      rows.value = res.data ?? [];
    }
  } finally {
    loading.value = false;
  }
};

const open = () => {
  visible.value = true;
  form.value = {};
  loadData();
  loadUsers();
};

const handleAdd = async () => {
  if (!form.value.delegateUserName) {
    message.warning('请选择被委托人');
    return;
  }
  adding.value = true;
  try {
    const res = await addDelegateApi({
      delegateUserName: form.value.delegateUserName,
      startTime: form.value.range?.[0] ? form.value.range[0].format('YYYY-MM-DD HH:mm:ss') : undefined,
      endTime: form.value.range?.[1] ? form.value.range[1].format('YYYY-MM-DD HH:mm:ss') : undefined,
      remark: form.value.remark?.trim() || undefined,
    });
    if (res.code !== 200) {
      message.error(res.msg || '添加失败');
      return;
    }
    message.success('委托规则已生效');
    form.value = {};
    loadData();
  } finally {
    adding.value = false;
  }
};

const handleToggleStatus = async (record: BpmDelegateRecord) => {
  const res = await changeDelegateStatusApi(record.id, record.status === 1 ? 0 : 1);
  if (res.code === 200) {
    loadData();
  } else {
    message.error(res.msg || '操作失败');
  }
};

const handleDelete = async (record: BpmDelegateRecord) => {
  const res = await deleteDelegateApi(record.id);
  if (res.code === 200) {
    message.success('已删除');
    loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};

defineExpose({ open });
</script>

<style scoped>
.bpm-delegate-tip {
  margin-bottom: 12px;
}

.bpm-delegate-form {
  margin-bottom: 12px;
  row-gap: 8px;
}

.bpm-danger-link {
  color: #ff4d4f;
}
</style>
