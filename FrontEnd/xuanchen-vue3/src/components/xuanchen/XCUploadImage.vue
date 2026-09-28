<template>
  <a-upload :file-list="innerFileList" :action="uploadUrl" :before-upload="beforeUpload"
    :custom-request="customRequest" @change="handleChange" :data="{ customPath: props.imagePath }"
    :list-type="props.listType" :max-count="props.maxCount" @preview="handlePreview">
    <div v-if="innerFileList.length < props.maxCount">
      <plus-outlined />
      <div style="margin-top: 8px">上传图片</div>
    </div>
  </a-upload>
  <a-modal :open="previewVisible" :title="previewTitle" :footer="null" @cancel="handleCancel">
    <img alt="XuanChen" style="width: 100%" :src="previewImage" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { UploadFile, UploadChangeParam } from 'ant-design-vue';
import { useAuthStore } from '@/stores';
import type { Result } from '@/types/api';
import { getImageView, FILE_VIEW_BASE_URL } from '@/utils/ImageUtil';
import { message } from 'ant-design-vue';
/** 定义变量 */
const authStore = useAuthStore();
// a-upload 的 action 是组件内部直发 XHR，不经过 axios 实例，需显式补 APP_BASE_URL 前缀（/api）
const uploadUrl = import.meta.env.APP_BASE_URL + import.meta.env.APP_FILE_UPLOAD_PATH;
const previewVisible = ref(false);
const previewImage = ref('');
const previewTitle = ref('');
// 组件内部展示用文件列表（受控），对外 v-model 只暴露服务端相对路径字符串数组，避免父子互相写 UploadFile 形成回环
const innerFileList = ref<UploadFile[]>([]);
// 定义 emits 用于向父组件传递更新后的服务端相对路径列表
const emit = defineEmits(['update:fileList']);
/** 接收变量 */
const props = defineProps({
  imagePath: { type: String, default: '' },
  // v-model 契约：服务端相对路径字符串数组，如 ['avatar/xxx_20260922.png']
  fileList: { type: Array as () => string[], default: () => [] },
  listType: { type: String, default: 'picture-card' }, // 自定义展示类型
  maxCount: { type: Number, default: 5 } // 最大上传数量
})
/** 方法 */
/** 去掉文件访问基址前缀，得到服务端相对路径；若是编码过的 URL 顺便解码回存储原文 */
const stripViewBase = (url: string): string => {
  const rest = url.startsWith(FILE_VIEW_BASE_URL) ? url.slice(FILE_VIEW_BASE_URL.length) : url;
  try {
    return decodeURI(rest);
  } catch {
    return rest;
  }
};

function getBase64(file: File) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });
}

// 兼容历史调用：外部可能传字符串、UploadFile 对象或完整预览 URL，统一抽取为服务端相对路径
const toRelativePath = (item: unknown): string | null => {
  if (typeof item === 'string') {
    return stripViewBase(item);
  }
  if (item && typeof item === 'object') {
    const p = (item as any)?.response?.data ?? (item as any)?.response?.msg ?? (item as any)?.url;
    if (typeof p === 'string' && p) {
      return stripViewBase(p);
    }
  }
  return null;
};

const extractPaths = (list: unknown[]): string[] =>
  list.map(toRelativePath).filter((v): v is string => !!v);

const toDisplayFile = (relativePath: string): UploadFile => ({
  uid: relativePath,
  name: relativePath.substring(relativePath.lastIndexOf('/') + 1),
  status: 'done',
  url: getImageView(relativePath) ?? '',
  response: { data: relativePath }
});

// 当前展示列表中已上传成功的服务端相对路径（上传中/失败的文件不计入对外 model）
const currentPaths = (): string[] =>
  innerFileList.value
    .filter(f => f.status === 'done')
    .map(f => ((f.response?.data || f.response?.msg) as string) || '')
    .filter(Boolean);

// 外部 → 内部：仅当外部相对路径与内部实际状态不一致时才整体替换，
// 内部 emit 引发的回声因内容一致被忽略，杜绝 watch 与 v-model 互相写的循环更新
watch(() => props.fileList, (val: unknown) => {
  const incoming = Array.isArray(val) ? extractPaths(val) : [];
  const current = currentPaths();
  const same = incoming.length === current.length && incoming.every((p, i) => p === current[i]);
  if (!same) {
    innerFileList.value = incoming.map(toDisplayFile);
  }
}, { deep: true, immediate: true });

const beforeUpload = (file: any) => {
  // 检查数量限制（失败/上传中的占位不计入有效数量）
  const validCount = innerFileList.value.filter(f => f.status !== 'error').length;
  if (validCount >= props.maxCount) {
    message.error(`最多只能上传 ${props.maxCount} 张图片`);
    return false;
  }
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    message.error('请上传jpg/png图片');
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('请上传小于2M的图片');
  }
  return isJpgOrPng && isLt2M;
};

// 自定义上传：在“真正发请求前”读取最新 token（token 存于 sessionStorage，非响应式，
// setup 时取一次会在重新登录后失效）；同时按响应体 code 收口服务端校验结果，
// 避免 HTTP 200 + code:400（文件类型/大小被服务端拒绝）被误判为上传成功
const customRequest = (option: any) => {
  const { action, file, filename = 'file', data, onProgress, onSuccess, onError } = option;
  const xhr = new XMLHttpRequest();
  const formData = new FormData();
  formData.append(filename, file as File, (file as File).name);
  if (data && typeof data === 'object') {
    Object.entries(data).forEach(([key, value]) => formData.append(key, String(value)));
  }
  xhr.open('POST', action as string, true);
  const token = authStore.getToken();
  if (token) {
    xhr.setRequestHeader('XC-ACCESS-TOKEN', token);
  }
  xhr.upload.onprogress = (e: ProgressEvent) => {
    if (e.lengthComputable) {
      onProgress?.({ percent: Math.round((e.loaded / e.total) * 100) }, file);
    }
  };
  xhr.onload = () => {
    let res: Result<string | null> | null = null;
    try {
      res = JSON.parse(xhr.responseText) as Result<string | null>;
    } catch {
      // 非 JSON 响应按失败处理
    }
    if (xhr.status >= 200 && xhr.status < 300 && res?.code === 200) {
      onSuccess?.(res, xhr);
    } else {
      onError?.(new Error(res?.msg || '上传失败'), res);
    }
  };
  xhr.onerror = () => {
    onError?.(new Error('上传失败，请检查网络或重新登录'));
  };
  xhr.send(formData);
  return {
    abort: () => xhr.abort()
  };
};

const handleChange = (info: UploadChangeParam) => {
  const status = info.file.status;
  if (status === 'uploading') {
    innerFileList.value = info.fileList;
    return;
  }
  if (status === 'done') {
    // 以服务端返回的相对路径为准（后端当前落在 response.msg，兼容未来放到 response.data 的实现），
    // 补全预览地址后回写内部展示列表
    innerFileList.value = info.fileList.map(f => {
      if (f.status === 'done' && !f.url) {
        const relativePath = (f.response?.data || f.response?.msg) as string;
        return relativePath ? { ...f, url: getImageView(relativePath) ?? '' } : f;
      }
      return f;
    });
    // 直接把服务端相对路径回写给父组件，父组件无需猜测 UploadFile/response 结构
    emit('update:fileList', currentPaths());
    return;
  }
  if (status === 'error') {
    innerFileList.value = info.fileList;
    message.error(info.file.response?.msg || info.file.error?.message || '上传失败');
    // 失败文件不进入对外 model，同步一次确保父组件拿到干净的路径列表
    emit('update:fileList', currentPaths());
    return;
  }
  if (status === 'removed') {
    innerFileList.value = info.fileList;
    emit('update:fileList', currentPaths());
  }
};

const handleCancel = () => {
  previewVisible.value = false;
  previewTitle.value = '';
};
const handlePreview = async (file: UploadFile) => {
  if (!file.url && !file.preview) {
    file.preview = (await getBase64(file.originFileObj!)) as string;
  }
  previewImage.value = (file.url || file.preview) as string;
  previewVisible.value = true;
  previewTitle.value = file.name || file.url?.substring(file.url.lastIndexOf('/') + 1) || '';
};
</script>
