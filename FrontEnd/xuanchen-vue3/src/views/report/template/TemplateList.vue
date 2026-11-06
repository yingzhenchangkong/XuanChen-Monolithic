<template>
  <XCListPage ref="listRef" :url="url" :columns="templateColumns" :query-form-items="templateQueryFormItems"
    :query-params="templateQueryParams" :buttons="{ add: true, deleteBatch: true }" export-title="报表模板"
    :status-api="changeTemplateStatusApi">
    <!-- 操作列：设计 / 复制 / 编辑 / 删除 -->
    <template #operation="{ record, handleEdit, handleDelete }">
      <a @click="handleDesign(record)">
        <HighlightOutlined /> 设计
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
      <a-popconfirm title="确定删除该模板吗？已建报表不受影响。" placement="left"
        @confirm="() => handleDelete(record.id)">
        <a>
          <DeleteOutlined /> 删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 类型列 / 状态列（点标签即启用↔停用，状态流转只在模板上做） -->
    <template #bodyCell="{ column, index, dataSource, handleStatusChange }">
      <template v-if="column.dataIndex === 'templateType'">
        {{ reportTypeText(dataSource[index].templateType as number) }}
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="dataSource[index].status === 1 ? 'green' : 'volcano'" :style="{ cursor: 'pointer' }"
          @click="handleStatusChange(dataSource[index], index)">
          {{ templateStatusText(dataSource[index].status as number) }}
        </a-tag>
      </template>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
    </template>
  </XCListPage>
</template>

<script setup lang="ts">
import { h, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Modal, message } from 'ant-design-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import Operation from './modal/Operation.vue';
import { TemplateApiUrl, copyTemplateApi, changeTemplateStatusApi } from './template.api';
import {
  templateQueryParams, templateQueryFormItems, templateColumns, templateStatusText,
} from './template.data';
import { reportTypeText } from '../report.data';

const router = useRouter();

const url = reactive({
  list: TemplateApiUrl.INDEX_LIST,
  delete: TemplateApiUrl.INDEX_DELETE,
  deleteBatch: TemplateApiUrl.INDEX_DELETE_BATCH,
});

const listRef = ref();

/** 设计：跳转全屏设计器页面（对齐在线表单设计器样式） */
const handleDesign = (record: Record<string, unknown>) => {
  router.push(`/report/template/design/${String(record.id ?? '')}`);
};

/** 复制：弹窗填写新编码/名称 */
const handleCopy = (record: Record<string, unknown>) => {
  let code = `${String(record.templateCode ?? '')}_copy`;
  let name = `${String(record.templateName ?? '')}-副本`;
  Modal.confirm({
    title: '复制模板',
    content: () =>
      h('div', null, [
        h('p', { style: 'margin:4px 0' }, '将复制该模板的设计内容，请填写新模板的编码与名称：'),
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
        message.warning('新模板编码/名称不能为空！');
        return Promise.reject();
      }
      const res = await copyTemplateApi(String(record.id), code.trim(), name.trim());
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
