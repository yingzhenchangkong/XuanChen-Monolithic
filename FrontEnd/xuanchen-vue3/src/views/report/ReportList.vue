<template>
  <XCListPage ref="listRef" :url="url" :columns="columns" :query-form-items="queryFormItems"
    :query-params="queryParams" :buttons="{ add: true, deleteBatch: true }" export-title="报表管理">
    <!-- 操作列：预览 / 复制 / 编辑 / 删除（状态流转只在「模板设计」里做） -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="handlePreview(record)">
        <EyeOutlined /> 预览
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleCopy(record)">
        <CopyOutlined /> 复制
      </a>
      <a-divider type="vertical"></a-divider>
      <a @click="handleEdit(record)">
        <EditOutlined /> 编辑
      </a>
      <a-divider type="vertical"></a-divider>
      <a-popconfirm title="删除报表不影响模板及其数据集，确定删除吗？" placement="left"
        @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 类型列 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'reportType'">
        {{ reportTypeText(record.reportType as number) }}
      </template>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
      <Preview v-model:open="previewOpen" :title="previewTitle" :report-code="previewCode" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { h, reactive, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import Operation from './modal/Operation.vue';
import Preview from './modal/Preview.vue';
import { ReportApiUrl, copyReportApi } from './report.api';
import { queryParams, queryFormItems, columns, reportTypeText } from './report.data';

const url = reactive({
  list: ReportApiUrl.INDEX_LIST,
  delete: ReportApiUrl.INDEX_DELETE,
  deleteBatch: ReportApiUrl.INDEX_DELETE_BATCH,
});

const listRef = ref();

/** 预览弹窗（默认全屏，可退出全屏） */
const previewOpen = ref(false);
const previewTitle = ref('');
const previewCode = ref('');
const handlePreview = (record: Record<string, unknown>) => {
  previewCode.value = String(record.reportCode ?? '');
  previewTitle.value = `预览 - ${String(record.reportName ?? '')}`;
  previewOpen.value = true;
};

/** 复制：弹窗填写新编码/名称 */
const handleCopy = (record: Record<string, unknown>) => {
  let code = `${String(record.reportCode ?? '')}_copy`;
  let name = `${String(record.reportName ?? '')}-副本`;
  Modal.confirm({
    title: '复制报表',
    content: () =>
      h('div', null, [
        h('p', { style: 'margin:4px 0' }, '将复制该报表的设计配置（版式/参数/绑定，数据集随模板维护），请填写新报表的编码与名称：'),
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
        message.warning('新报表编码/名称不能为空！');
        return Promise.reject();
      }
      const res = await copyReportApi(String(record.id), code.trim(), name.trim());
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
