<template>
  <a-card :bordered="false" title="配额管理">
    <template #extra>
      <a-button @click="loadData">
        <template #icon>
          <ReloadOutlined />
        </template>刷新
      </a-button>
    </template>

    <a-alert type="info" show-icon class="quota-tip"
      message="未单独设置的用户使用系统默认配额（可在 application.yml 的 xuanchen.fileinfo.default-quota-bytes 调整）；配额按用户名下有效文件总大小计算。" />

    <a-table :data-source="rows" :columns="columns" row-key="username" size="middle"
      :loading="loading" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'username'">
          {{ record.username }}
        </template>
        <template v-else-if="column.dataIndex === 'usedBytes'">
          {{ formatFileSize(record.usedBytes as number) }}
        </template>
        <template v-else-if="column.dataIndex === 'quotaBytes'">
          <a-tag v-if="Number(record.quotaBytes) === 0" color="green">不限制</a-tag>
          <a-tag v-else-if="Number(record.quotaBytes) === -1">默认</a-tag>
          <span v-else>{{ formatFileSize(record.quotaBytes as number) }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'edit'">
          <a-input-number v-model:value="editValues[record.username]" :min="0" :max="10240" :step="1"
            style="width: 110px" addon-after="GB" :disabled="Number(record.quotaBytes) === 0" />
          <a-button type="link" size="small" :disabled="Number(record.quotaBytes) === 0"
            @click="handleSave(record.username as string, editValues[record.username] as number)">
            保存
          </a-button>
          <a-popconfirm v-if="Number(record.quotaBytes) !== -1 && Number(record.quotaBytes) !== 0"
            title="恢复为系统默认配额？" @confirm="() => handleSave(record.username as string, -1)">
            <a-button type="link" size="small">恢复默认</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { ReloadOutlined } from '@ant-design/icons-vue';

import { formatFileSize } from '../filemanage.data';
import { getQuotaListApi, saveQuotaApi } from '../filemanage.api';
import type { QuotaUsageRecord } from '../filemanage.api';

const rows = ref<QuotaUsageRecord[]>([]);
const loading = ref(false);
const editValues = reactive<Record<string, number>>({});

const columns = [
  { title: '用户', dataIndex: 'username', width: 200 },
  { title: '已用空间', dataIndex: 'usedBytes', width: 140, align: 'right' as const },
  { title: '当前配额', dataIndex: 'quotaBytes', width: 140, align: 'center' as const },
  { title: '调整配额（GB，0=不限制）', dataIndex: 'edit', align: 'left' as const },
];

const GB = 1024 * 1024 * 1024;

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getQuotaListApi();
    if (res.code === 200) {
      rows.value = res.data || [];
      //编辑框初值：字节 → GB（未设置/默认的取已用向上取整作为提示起点）
      rows.value.forEach((row) => {
        if (row.quotaBytes > 0) {
          editValues[row.username] = Math.ceil(row.quotaBytes / GB);
        } else {
          editValues[row.username] = Math.max(1, Math.ceil((row.usedBytes || 0) / GB));
        }
      });
    } else {
      message.error(res.msg || '加载配额总览失败');
    }
  } finally {
    loading.value = false;
  }
};

const handleSave = async (username: string, gb: number) => {
  const quotaBytes = gb < 0 ? -1 : Math.round(gb * GB);
  const res = await saveQuotaApi(username, quotaBytes);
  if (res.code === 200) {
    message.success(quotaBytes === -1 ? '已恢复默认配额！' : '配额已保存！');
    await loadData();
  } else {
    message.error(res.msg || '配额保存失败');
  }
};

onMounted(loadData);
</script>

<style scoped>
.quota-tip {
  margin-bottom: 16px;
}
</style>
