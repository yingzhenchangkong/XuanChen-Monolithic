<template>
  <a-upload :file-list="innerFileList" :accept="props.accept" :max-count="props.maxCount"
    :before-upload="beforeUpload" :custom-request="customRequest" @change="handleChange" @remove="handleRemove">
    <a-button>
      <template #icon>
        <UploadOutlined />
      </template>{{ props.buttonText }}
    </a-button>
  </a-upload>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { UploadOutlined } from '@ant-design/icons-vue';
import type { UploadChangeParam, UploadFile } from 'ant-design-vue';

import { uploadFileInfoApi } from '@/views/filemanage/filemanage.api';
import type { FileInfoSummary } from '@/views/filemanage/filemanage.api';

/**
 * 通用文件上传组件（文件中心）
 * 对外 v-model:modelValue 契约：已上传文件摘要数组 [{ id, name, previewUrl }]，
 * 父组件拿 id 即可通过 /fileinfo/url 换取带有效期的预签名地址，禁止缓存 URL。
 */

export interface UploadedFileInfo {
  id: string;
  name: string;
  previewUrl: string;
}

const props = defineProps({
  /** v-model：已上传文件摘要数组 */
  modelValue: { type: Array as () => UploadedFileInfo[], default: () => [] },
  /** 业务类型（objectKey 首段）：common/avatar/photo/convert... */
  bizType: { type: String, default: 'common' },
  /** 允许的文件类型（逗号分隔 MIME 或扩展名，交由浏览器过滤；服务端仍做强校验） */
  accept: { type: String, default: '' },
  /** 最大文件大小（MB），上限 50 与后端一致 */
  maxSize: { type: Number, default: 50 },
  /** 最大上传数量 */
  maxCount: { type: Number, default: 5 },
  /** 按钮文案 */
  buttonText: { type: String, default: '上传文件' },
});

const emit = defineEmits(['update:modelValue']);

const innerFileList = ref<UploadFile[]>([]);

// 外部 → 内部：内容一致时忽略回声，避免 watch 与 v-model 循环更新
watch(() => props.modelValue, (val: UploadedFileInfo[]) => {
  const incoming = Array.isArray(val) ? val : [];
  const current = currentSummaries();
  const same = incoming.length === current.length
    && incoming.every((p, i) => p.id === current[i]?.id);
  if (!same) {
    innerFileList.value = incoming.map(f => ({
      uid: f.id,
      name: f.name,
      status: 'done' as const,
      response: { data: f },
    }));
  }
}, { deep: true, immediate: true });

/** 当前上传成功文件对应的摘要列表 */
const currentSummaries = (): UploadedFileInfo[] =>
  innerFileList.value
    .filter(f => f.status === 'done')
    .map(f => (f.response?.data as UploadedFileInfo | undefined))
    .filter((f): f is UploadedFileInfo => !!f?.id);

const beforeUpload = (file: File) => {
  const validCount = innerFileList.value.filter(f => f.status !== 'error').length;
  if (validCount >= props.maxCount) {
    message.error(`最多只能上传 ${props.maxCount} 个文件`);
    return false;
  }
  if (file.size / 1024 / 1024 >= props.maxSize) {
    message.error(`文件大小不能超过 ${props.maxSize}MB`);
    return false;
  }
  return true;
};

/** 自定义上传：走 axios 实例（自动带 token 与 /api 前缀），按响应体 code 收口结果 */
const customRequest = async (option: { file: File; onSuccess?: (body: unknown) => void; onError?: (e: Error) => void }) => {
  const formData = new FormData();
  formData.append('file', option.file);
  formData.append('bizType', props.bizType);
  try {
    const res = await uploadFileInfoApi(formData);
    if (res.code === 200 && res.data) {
      option.onSuccess?.({ data: res.data });
    } else {
      option.onError?.(new Error(res.msg || '上传失败'));
    }
  } catch {
    option.onError?.(new Error('上传失败，请检查网络或重新登录'));
  }
};

const handleChange = (info: UploadChangeParam) => {
  innerFileList.value = info.fileList;
  if (info.file.status === 'done') {
    message.success(`「${info.file.name}」上传成功！`);
    emit('update:modelValue', currentSummaries());
  } else if (info.file.status === 'error') {
    message.error(info.file.error?.message || '上传失败');
    emit('update:modelValue', currentSummaries());
  } else if (info.file.status === 'removed') {
    emit('update:modelValue', currentSummaries());
  }
};

const handleRemove = () => true;
</script>
