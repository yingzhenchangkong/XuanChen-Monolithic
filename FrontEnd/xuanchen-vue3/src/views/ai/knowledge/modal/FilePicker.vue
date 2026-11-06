<template>
  <a-modal v-model:open="visible" title="从文件中心导入" :width="720" @ok="handleOk" :confirm-loading="saving"
    ok-text="导入所选并解析" destroy-on-close>
    <div style="display: flex; gap: 8px; margin-bottom: 12px">
      <a-input-search v-model:value="keyword" placeholder="按文件名搜索" style="width: 280px" allow-clear
        @search="() => loadData(1)" />
      <span class="picker-tip">仅支持 pdf / doc / docx / xls / xlsx / ppt / pptx / txt / md；不可解析的文件已置灰</span>
    </div>
    <a-table :data-source="dataSource" :columns="columns" :loading="loading" row-key="id" size="small" bordered
      :pagination="pagination" @change="onTableChange"
      :row-selection="rowSelection">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'fileSize'">
          {{ formatSize(record.fileSize) }}
        </template>
        <template v-else-if="column.dataIndex === 'disabledReason'">
          <a-tag v-if="rowDisabledReason(record)" color="orange">{{ rowDisabledReason(record) }}</a-tag>
          <a-tag v-else color="green">可导入</a-tag>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { TableRowSelection } from 'ant-design-vue/es/table/interface';

import { getAction } from '@/utils/httpAction';
import { importFromCenterApi } from '../knowledge.api';
import { formatSize } from '../knowledge.data';
import type { AiKbRecord, AiDocRecord, FileCenterRecord } from '../knowledge.types';
import type { Result, PageResult } from '@/types/api';

/** 知识库可解析的扩展名白名单（与后端 KB_PARSEABLE_EXTS 保持一致，白名单方式更安全） */
const KB_PARSEABLE_EXTS = new Set([
  'pdf',
  'doc', 'docx',
  'xls', 'xlsx',
  'ppt', 'pptx',
  'txt', 'md', 'markdown',
]);

const props = defineProps<{ kb: AiKbRecord | null; docs: AiDocRecord[] }>();
const open = defineModel<boolean>('open', { default: false });
const emit = defineEmits<{ (e: 'saved'): void }>();

const visible = open;
const loading = ref(false);
const saving = ref(false);
const keyword = ref('');
const dataSource = ref<FileCenterRecord[]>([]);
const selectedRowKeys = ref<string[]>([]);
const pagination = reactive({ current: 1, pageSize: 8, total: 0, showSizeChanger: false });

const columns = [
  { title: '文件名', dataIndex: 'originalName', ellipsis: true },
  { title: '类型', dataIndex: 'ext', width: 70 },
  { title: '大小', dataIndex: 'fileSize', width: 90 },
  { title: '导入时间', dataIndex: 'createTime', width: 110 },
  { title: '状态', dataIndex: 'disabledReason', width: 90 },
];

const rowDisabledReason = (record: FileCenterRecord): string | null => {
  const ext = (record.ext || '').toLowerCase();
  if (!KB_PARSEABLE_EXTS.has(ext)) return '不支持该格式';
  if (props.docs.some((d) => d.fileId === record.id)) return '已导入';
  return null;
};

const rowSelection = computed<TableRowSelection>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys as string[];
  },
  getCheckboxProps: (record: FileCenterRecord) => ({
    disabled: !!rowDisabledReason(record),
  }),
}));

watch(visible, (val) => {
  if (val) {
    selectedRowKeys.value = [];
    keyword.value = '';
    loadData(1);
  }
});

const loadData = async (pageNo?: number) => {
  loading.value = true;
  try {
    const res = await getAction<PageResult<FileCenterRecord>>('/fileinfo/my', {
      pageNo: pageNo ?? pagination.current,
      pageSize: pagination.pageSize,
      keyword: keyword.value || undefined,
    });
    if (res.code === 200) {
      dataSource.value = res.data?.records || [];
      pagination.current = res.data?.current || pagination.current;
      pagination.total = res.data?.total || 0;
    }
  } finally {
    loading.value = false;
  }
};

const onTableChange = (pag: { current?: number; pageSize?: number }) => {
  if (pag.current) pagination.current = pag.current;
  if (pag.pageSize) pagination.pageSize = pag.pageSize;
  loadData();
};

const handleOk = async () => {
  if (!props.kb?.id) return;
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先勾选要导入的文件');
    return;
  }
  saving.value = true;
  try {
    const res = await importFromCenterApi(props.kb.id, selectedRowKeys.value);
    if (res.code === 200) {
      message.success(res.data || '导入成功');
      visible.value = false;
      emit('saved');
    } else {
      message.error(res.msg || '导入失败');
    }
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.picker-tip {
  color: #999;
  font-size: 12px;
  align-self: center;
}
</style>
