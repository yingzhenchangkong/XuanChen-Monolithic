<template>
  <XCListPage ref="listRef" :url="url" :columns="columns" :query-form-items="queryFormItems"
    :query-params="queryParams" :buttons="{ add: true, deleteBatch: true }" export-title="数据大屏">
    <!-- 操作列：预览 / 发布|停用 / 复制 / 编辑 / 删除（设计入口统一在「数据大屏设计」画廊） -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a v-if="record.status === 1" @click="handlePreview(record)">
        <EyeOutlined /> 预览
      </a>
      <span v-else class="op-disabled">
        <EyeOutlined /> 预览
      </span>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm v-if="record.status === 1" title="确定停用该大屏吗？" placement="left"
        @confirm="() => handleUnpublish(record.id)">
        <a>
          <PauseCircleOutlined /> 停用
        </a>
      </a-popconfirm>
      <a-popconfirm v-else title="确定发布该大屏吗？" placement="left" @confirm="() => handlePublish(record.id)">
        <a>
          <SendOutlined /> 发布
        </a>
      </a-popconfirm>
      <a-divider type="vertical"></a-divider>
      <a @click="handleCopy(record)">
        <CopyOutlined /> 复制
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleEdit(record)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="删除大屏会同时删除其数据集，确定删除吗？" placement="left"
        @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 状态列 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="screenStatusColor(record.status as number)">
          {{ screenStatusText(record.status as number) }}
        </a-tag>
      </template>
    </template>

    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { h, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Modal, message } from 'ant-design-vue';
import {
  CopyOutlined, DeleteOutlined, EditOutlined, EyeOutlined,
  PauseCircleOutlined, SendOutlined,
} from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import Operation from './modal/Operation.vue';
import { VisualizationApiUrl, copyScreenApi, publishScreenApi, unpublishScreenApi } from './visualization.api';
import { columns, queryFormItems, queryParams, screenStatusColor, screenStatusText } from './visualization.data';

const router = useRouter();
const listRef = ref();

const url = reactive({
  list: VisualizationApiUrl.INDEX_LIST,
  delete: VisualizationApiUrl.INDEX_DELETE,
  deleteBatch: VisualizationApiUrl.INDEX_DELETE_BATCH,
});

/** 预览：新窗口打开展示页，方便在大屏/副屏上查看 */
const handlePreview = (record: Record<string, unknown>) => {
  const code = String(record.screenCode ?? '');
  if (!code) return;
  const href = router.resolve({ name: 'visualizationViewer', params: { code } }).href;
  window.open(href, '_blank');
};

const handlePublish = async (id?: string) => {
  if (!id) return;
  const res = await publishScreenApi(id);
  if (res.code === 200) {
    message.success('发布成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '发布失败');
  }
};

const handleUnpublish = async (id?: string) => {
  if (!id) return;
  const res = await unpublishScreenApi(id);
  if (res.code === 200) {
    message.success('停用成功！');
    listRef.value?.loadData();
  } else {
    message.error(res.msg || '停用失败');
  }
};

/** 复制：弹窗填写新编码/名称 */
const handleCopy = (record: Record<string, unknown>) => {
  let code = `${String(record.screenCode ?? '')}_copy`;
  let name = `${String(record.screenName ?? '')}-副本`;
  Modal.confirm({
    title: '复制大屏',
    content: () =>
      h('div', null, [
        h('p', { style: 'margin:4px 0' }, '将复制该大屏的画布配置与数据集，请填写新大屏的编码与名称：'),
        h('input', {
          value: code,
          style: 'width:100%;margin-bottom:8px;padding:4px 8px;border:1px solid #d9d9d9;border-radius:4px',
          onInput: (e: Event) => (code = (e.target as HTMLInputElement).value),
        }),
        h('input', {
          value: name,
          style: 'width:100%;padding:4px 8px;border:1px solid #d9d9d9;border-radius:4px',
          onInput: (e: Event) => (name = (e.target as HTMLInputElement).value),
        }),
      ]),
    onOk: async () => {
      if (!code.trim() || !name.trim()) {
        message.warning('新大屏编码/名称不能为空！');
        return Promise.reject();
      }
      const res = await copyScreenApi(String(record.id), code.trim(), name.trim());
      if (res.code === 200) {
        message.success('复制成功！');
        listRef.value?.loadData();
      } else {
        message.error(res.msg || '复制失败');
        return Promise.reject();
      }
    },
  });
};
</script>

<style scoped>
.op-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
