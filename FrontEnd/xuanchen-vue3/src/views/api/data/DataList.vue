<template>
  <XCListPage ref="listRef" :url="url" :columns="columns" :query-form-items="queryFormItems"
    :query-params="queryParams" :selection="false" export-title="采集数据">
    <template #data="{ record }">
      <a-tooltip :title="(record as ApiDataRecord).data" placement="topLeft">
        <span class="data-cell">{{ (record as ApiDataRecord).data }}</span>
      </a-tooltip>
    </template>

    <template #operation="{ record }">
      <a @click="showDetail(record as ApiDataRecord)">
        <EyeOutlined /> 详情
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="删除该条采集数据？" placement="left" @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>
  </XCListPage>

  <!-- 数据详情弹窗 -->
  <a-modal v-model:open="detailOpen" title="采集数据详情" :footer="null" width="720px">
    <pre class="detail-pre">{{ detailText }}</pre>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { DeleteOutlined, EyeOutlined } from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import { deleteDataApi, getDataListUrl } from '../designer.api';
import type { ApiDataRecord } from '../designer.api';

const listRef = ref();

const url = {
  list: getDataListUrl,
};

const queryParams = reactive<Record<string, unknown>>({
  apiCode: '',
  pageNo: 1,
  pageSize: 10,
});

const queryFormItems = reactive([
  {
    name: 'apiCode',
    label: '接口编码',
    type: 'input' as const,
    placeholder: '按来源接口编码筛选',
  },
]);

const columns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  { title: '接口编码', dataIndex: 'apiCode', align: 'left', width: 180, ellipsis: true },
  { title: '数据', key: 'data', align: 'left', ellipsis: true },
  { title: '采集时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 130, fixed: 'right' as const },
];

// ---------- 详情 ----------
const detailOpen = ref(false);
const detailText = ref('');

const showDetail = (record: ApiDataRecord) => {
  try {
    detailText.value = JSON.stringify(JSON.parse(record.data ?? '{}'), null, 2);
  } catch {
    detailText.value = record.data ?? '';
  }
  detailOpen.value = true;
};

const handleDelete = async (id: string) => {
  const res = await deleteDataApi(id);
  if (res.code === 200) {
    message.success('删除成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};
</script>

<style scoped>
.data-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.detail-pre {
  max-height: 60vh;
  overflow: auto;
  background: var(--ant-color-fill-quaternary, #f6f6f6);
  border-radius: 6px;
  padding: 8px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
