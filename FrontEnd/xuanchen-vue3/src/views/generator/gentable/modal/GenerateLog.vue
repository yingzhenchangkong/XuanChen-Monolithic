<template>
  <a-modal v-model:open="visible" title="生成记录" width="900" :footer="null">
    <div class="btn-style">
      <a-button @click="loadData">
        <template #icon>
          <ReloadOutlined />
        </template>刷新
      </a-button>
      <a-popconfirm title="确定清空当前表配置的全部生成记录吗？" @confirm="handleClear">
        <a-button danger :disabled="!logs.length">清空记录</a-button>
      </a-popconfirm>
      <span v-if="tableName" style="margin-left: 8px;color: #8c8c8c">表：{{ tableName }}</span>
    </div>
    <a-table :data-source="logs" :columns="columns" :pagination="pagination" :loading="loading" row-key="id"
      size="small" bordered @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'success'">
          <a-tag :color="record.success ? 'green' : 'red'">{{ record.success ? '成功' : '失败' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'generateType'">
          {{ typeText(record.generateType) }}
        </template>
        <template v-else-if="column.dataIndex === 'fileList'">
          <a v-if="record.fileList" @click="showFiles(record)">{{ record.fileCount || 0 }} 个文件</a>
          <span v-else style="color: #bfbfbf">-</span>
        </template>
        <template v-else-if="column.dataIndex === 'message'">
          <span :title="record.message" class="log-message">{{ record.message }}</span>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { h, reactive, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';
import { clearGenerateLogs, getGenerateLogs } from '../gentable.api';
import type { GenGenerateLog } from '../gentable.types';

const visible = ref(false);
const loading = ref(false);
const logs = ref<GenGenerateLog[]>([]);
const tableId = ref('');
const tableName = ref('');

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
  showSizeChanger: true,
});

const columns = [
  { title: '时间', dataIndex: 'createTime', width: 165 },
  { title: '方式', dataIndex: 'generateType', width: 90, align: 'center' },
  { title: '模板', dataIndex: 'template', width: 90, align: 'center' },
  { title: '结果', dataIndex: 'success', width: 80, align: 'center' },
  { title: '文件数', dataIndex: 'fileList', width: 110, align: 'center' },
  { title: '操作人', dataIndex: 'createBy', width: 100, align: 'center' },
  { title: '说明', dataIndex: 'message' },
];

const typeText = (type?: string) => {
  if (type === 'DISK') return '落盘生成';
  if (type === 'ZIP') return '打包下载';
  if (type === 'PREVIEW') return '预览';
  return type || '-';
};

const loadData = async () => {
  if (!tableId.value) return;
  loading.value = true;
  try {
    const res = await getGenerateLogs(tableId.value, pagination.current, pagination.pageSize);
    logs.value = res.records || [];
    pagination.total = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (page: { current: number; pageSize: number }) => {
  pagination.current = page.current;
  pagination.pageSize = page.pageSize;
  loadData();
};

const handleClear = async () => {
  const res = await clearGenerateLogs(tableId.value);
  if (res.code === 200) {
    message.success(res.msg || '已清空');
    pagination.current = 1;
    loadData();
  } else {
    message.error(res.msg || '清空失败');
  }
};

const showFiles = (record: GenGenerateLog) => {
  Modal.info({
    title: `本次生成的文件（${record.fileCount || 0} 个）`,
    width: 680,
    content: h('div', { style: 'max-height:340px;overflow:auto;font-size:12px;line-height:20px' },
      (record.fileList || '').split('\n').map((line) => h('div', line))),
  });
};

/** 打开记录弹窗 */
const show = (id: string, name?: string) => {
  tableId.value = id;
  tableName.value = name || '';
  pagination.current = 1;
  logs.value = [];
  visible.value = true;
  loadData();
};

defineExpose({ show });
</script>

<style lang="scss" scoped>
.log-message {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
