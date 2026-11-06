<template>
  <a-drawer v-model:open="visible" :title="`文档管理 - ${kb?.kbName ?? ''}`" placement="right" :width="860"
    :body-style="{ padding: '12px' }" @after-open-change="onOpenChange">
    <!-- 工具栏：上传 / 手动录入 / 文件中心导入 -->
    <div class="btn-style">
      <a-space wrap>
        <a-upload :customRequest="handleUpload" :show-upload-list="false" :disabled="uploading"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md">
          <a-button type="primary" :loading="uploading">
            <template #icon>
              <UploadOutlined />
            </template>上传文档
          </a-button>
        </a-upload>
        <a-button @click="textOpen = true">
          <template #icon>
            <FormOutlined />
          </template>录入内容
        </a-button>
        <a-button @click="pickerOpen = true">
          <template #icon>
            <FolderOpenOutlined />
          </template>从文件中心导入
        </a-button>
      </a-space>
      <span class="doc-tip">仅支持 pdf / doc / docx / xls / xlsx / ppt / pptx / txt / md，单文件 ≤ 50MB；不可解析的文档（如图片、压缩包、CAD）无法上传</span>
    </div>

    <!-- 手动录入弹窗 -->
    <DocTextInput v-model:open="textOpen" :kb="kb" @saved="loadData" />
    <!-- 文件中心导入弹窗 -->
    <DocFilePicker v-model:open="pickerOpen" :kb="kb" :docs="dataSource" @saved="loadData" />

    <!-- 文档表格 -->
    <a-table :data-source="dataSource" :columns="columnsDoc" :loading="loading" row-key="id" bordered size="small"
      :pagination="false">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="parseStatusColor(record.status)">
            <LoadingOutlined v-if="record.status === 'parsing'" />
            {{ parseStatusText(record.status) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'errorMsg'">
          <a-tooltip :title="record.errorMsg">
            <span>{{ record.errorMsg || '-' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-popconfirm :title="parseConfirmTitle(record)" placement="left" @confirm="handleParse(record, index)">
            <a>
              <CloudUploadOutlined />
              {{ record.status === 'ready' ? '重新解析' : '解析' }}
            </a>
          </a-popconfirm>
          <a-divider type="vertical"></a-divider>
          <a-popconfirm title="确定删除该文档吗？（向量同步清理）" placement="left" @confirm="handleDelete(record)">
            <a>
              <DeleteOutlined /> 删除
            </a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script lang="ts" setup>
import { ref, watch, onUnmounted } from 'vue';
import { message } from 'ant-design-vue';

import { uploadAction } from '@/utils/httpAction';
import { listDocsApi, parseDocApi, deleteDocApi } from './knowledge.api';
import { columnsDoc, parseStatusText, parseStatusColor } from './knowledge.data';
import type { AiKbRecord, AiDocRecord } from './knowledge.types';
import DocTextInput from './modal/TextInput.vue';
import DocFilePicker from './modal/FilePicker.vue';

/** ant-design-vue 4 未导出 UploadRequestOption，用结构化等价类型 */
interface CustomUploadOption {
  file: File | Blob | string;
}

const props = defineProps<{ kb: AiKbRecord | null }>();
const open = defineModel<boolean>('open', { default: false });

const visible = open;
const uploading = ref(false);
const loading = ref(false);
const dataSource = ref<AiDocRecord[]>([]);
const textOpen = ref(false);
const pickerOpen = ref(false);

/** 打开时加载并启动解析状态轮询 */
const onOpenChange = (opened: boolean) => {
  if (opened) {
    loadData();
    startPolling();
  } else {
    stopPolling();
  }
};

const loadData = async () => {
  if (!props.kb?.id) return;
  loading.value = true;
  try {
    const res = await listDocsApi(props.kb.id);
    if (res.code === 200) {
      dataSource.value = res.data?.records || [];
    }
  } finally {
    loading.value = false;
  }
};

/** 解析是分钟级异步任务：有 parsing/pending 行时每 5 秒轮询刷新 */
let timer: number | null = null;
const startPolling = () => {
  stopPolling();
  timer = window.setInterval(() => {
    if (dataSource.value.some((d) => d.status === 'parsing' || d.status === 'pending')) {
      loadData();
    }
  }, 5000);
};
const stopPolling = () => {
  if (timer !== null) {
    window.clearInterval(timer);
    timer = null;
  }
};
onUnmounted(stopPolling);

/** 知识库支持解析的文件扩展名白名单（与后端 KB_PARSEABLE_EXTS 保持一致） */
const KB_PARSEABLE_EXTS = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md'];
const KB_MAX_SIZE = 50 * 1024 * 1024; // 50MB

/** 从文件名提取小写扩展名 */
const getExt = (filename: string): string => {
  const idx = filename.lastIndexOf('.');
  return idx >= 0 ? filename.substring(idx + 1).toLowerCase() : '';
};

const handleUpload = async (options: CustomUploadOption) => {
  const { file } = options;
  if (!props.kb?.id) return;
  const fileObj = file as File;
  // 前端扩展名校验（双重保障：accept 属性可能被绕过）
  const filename = fileObj.name || '';
  const ext = getExt(filename);
  if (!KB_PARSEABLE_EXTS.includes(ext)) {
    message.error(`不支持上传该格式（${ext || '无扩展名'}），仅支持：${KB_PARSEABLE_EXTS.join(' / ')}`);
    return;
  }
  // 前端大小校验
  if (fileObj.size && fileObj.size > KB_MAX_SIZE) {
    message.error('文件大小超出 50MB 限制！');
    return;
  }
  uploading.value = true;
  try {
    const form = new FormData();
    form.append('file', file as File);
    form.append('kbId', props.kb.id);
    form.append('bizType', 'ai-kb');
    const res = await uploadAction<null>('/ai/kb/doc/upload', form);
    if (res.code === 200) {
      message.success('上传成功');
      loadData();
    } else {
      message.error(res.msg || '上传失败');
    }
  } finally {
    uploading.value = false;
  }
};

const handleParse = async (record: AiDocRecord, index: number) => {
  const res = await parseDocApi(record.id);
  if (res.code === 200) {
    message.success('已提交解析任务');
    dataSource.value[index].status = 'parsing';
    loadData();
  } else {
    message.error(res.msg || '提交失败');
  }
};

/** 解析确认文案：parsing 状态允许强制重试（后端有 10 分钟卡死看护，未超时会拒绝） */
const parseConfirmTitle = (record: AiDocRecord): string => {
  if (record.status === 'parsing') {
    return '该文档解析中。若长时间无进展（线程被挂起/中断），可强制重新解析？';
  }
  return record.status === 'ready' ? '确认重新解析？（先清旧向量，大文档为分钟级异步任务）' : '确认解析该文档？（大文档为分钟级异步任务）';
};

const handleDelete = async (record: AiDocRecord) => {
  const res = await deleteDocApi(record.id);
  if (res.code === 200) {
    message.success('删除成功');
    loadData();
  } else {
    message.error(res.msg || '删除失败');
  }
};

// kb 变化时刷新
watch(() => props.kb?.id, (val) => {
  if (val && visible.value) loadData();
});
</script>

<style scoped>
.doc-tip {
  color: #999;
  font-size: 12px;
  margin-left: 8px;
}
</style>
