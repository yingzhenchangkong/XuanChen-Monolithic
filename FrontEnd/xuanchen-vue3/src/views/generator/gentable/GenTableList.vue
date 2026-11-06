<template>
  <!-- 列表页按项目规范使用 XCListPage：查询区 / 工具栏 / 表格 / 回收站 / 弹窗插槽 -->
  <XCListPage ref="refListPage" :url="url" :columns="columnsIndex" :query-form-items="queryFormItems"
    :query-params="queryParams" :buttons="{ recycleBin: true, deleteBatch: true }" export-title="代码生成"
    @recycle-bin="handleRecycleBin">
    <!-- 工具栏：新增改为全屏独立页（对齐在线表单设计器样式），编辑保留弹窗 -->
    <template #toolbar-prepend>
      <a-button type="primary" @click="router.push('/generator/design')">
        <template #icon>
          <PlusOutlined />
        </template>新增
      </a-button>
    </template>

    <!-- 行操作：代码生成 / 预览 / 下载 / 生成记录 + 编辑 / 删除 -->
    <template #operation="{ record }">
      <a @click="handleGenerator(record)">
        <DatabaseOutlined />生成
      </a>
      <a-divider type="vertical" />
      <a @click="handlePreview(record)">
        <EyeOutlined />预览
      </a>
      <a-divider type="vertical" />
      <a @click="handleDownloadZip(record)">
        <DownloadOutlined />下载
      </a>
      <a-divider type="vertical" />
      <a @click="handleLogs(record)">
        <HistoryOutlined />记录
      </a>
      <a-divider type="vertical" />
      <a @click="refListPage?.handleEdit(record)">
        <EditOutlined />编辑
      </a>
      <a-divider type="vertical" />
      <a-popconfirm title="确定删除吗？" placement="left" @confirm="refListPage?.handleDelete(record.id)">
        <a>
          <DeleteOutlined />删除
        </a>
      </a-popconfirm>
    </template>

    <!-- 弹窗区域 -->
    <template #modals="{ operationTitle, setOperationRef, loadData }">
      <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData" />
      <RecycleBin ref="refRecycleBin" :url="recycleUrl" :columns="columnsRecBin" :query-form-items="queryFormItems"
        :query-params="queryParams" @childOK="loadData" />
    </template>
  </XCListPage>

  <!-- 生成预览 / 生成记录 -->
  <Preview ref="refPreview" />
  <GenerateLog ref="refLog" />
</template>

<script setup lang="ts">
import { h, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Modal, message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';

import XCListPage from '@/components/xuanchen/XCListPage.vue';
import RecycleBin from '@/components/xuanchen/XCRecycleBin.vue';
import Operation from './modal/Operation.vue';
import Preview from './modal/Preview.vue';
import GenerateLog from './modal/GenerateLog.vue';

import { GenTableApiUrl, generator, downloadZip, previewCode } from './gentable.api';
import { queryParams, queryFormItems, columnsIndex, columnsRecBin } from './gentable.data';
import type { GenTable } from './gentable.types';

const router = useRouter();

const refListPage = ref<any>(null);
const refPreview = ref<any>(null);
const refLog = ref<any>(null);
const refRecycleBin = ref<any>(null);

/** 列表接口 */
const url = reactive({
  list: GenTableApiUrl.INDEX_LIST,
  delete: GenTableApiUrl.INDEX_DELETE,
  deleteBatch: GenTableApiUrl.INDEX_DELETE_BATCH,
});

/** 回收站接口 */
const recycleUrl = {
  list: GenTableApiUrl.RECBIN_LIST,
  delete: GenTableApiUrl.RECBIN_DELETE,
  deleteBatch: GenTableApiUrl.RECBIN_DELETE_BATCH,
  revert: GenTableApiUrl.RECBIN_REVERT,
  revertBatch: GenTableApiUrl.RECBIN_REVERT_BATCH,
};

/** 回收站 */
const handleRecycleBin = () => {
  refRecycleBin.value?.show();
};

/** 代码生成：按当前行的表配置 id 触发，数据源与输出配置由后端按 id 组装 */
const handleGenerator = async (record: GenTable) => {
  if (!record.id) {
    return;
  }
  const res = await generator(record.id);
  const files = Array.isArray(res.data) ? res.data : [];
  if (!files.length) {
    message.success(res.msg || '生成成功');
    return;
  }
  // 把实际写入的文件列出来（含新增与覆盖更新），避免"生成成功"却不知道文件去了哪
  Modal.success({
    title: `生成成功：本次写入 ${files.length} 个文件（含新增与更新）`,
    width: 680,
    content: h(
      'div',
      { style: 'max-height:340px;overflow:auto;font-size:12px;line-height:20px' },
      files.map((f) => h('div', f)),
    ),
  });
};

/** 生成预览：临时目录渲染，确认无误后再落盘 */
const handlePreview = async (record: GenTable) => {
  if (!record.id) {
    return;
  }
  const files = await previewCode(record.id);
  if (!files.length) {
    message.warning('没有可预览的文件，请检查表配置');
    return;
  }
  refPreview.value?.show(files);
};

/** 生成代码并打包 zip 下载（产物含 menu.sql，不占用服务器输出目录） */
const handleDownloadZip = async (record: GenTable) => {
  if (!record.id) {
    return;
  }
  await downloadZip(record.id, record.tableName || '');
};

/** 查看生成记录 */
const handleLogs = (record: GenTable) => {
  if (!record.id) {
    return;
  }
  refLog.value?.show(record.id, record.tableName);
};
</script>
