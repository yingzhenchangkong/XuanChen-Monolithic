<template>
  <a-upload v-model:file-list="fileList" :action="uploadUrl" :headers="headers" :before-upload="beforeUpload"
    @change="handleChange" :data="{ customPath: props.imagePath }" :list-type="props.listType"
    :max-count="props.maxCount" @preview="handlePreview">
    <div v-if="fileList.length < props.maxCount">
      <plus-outlined />
      <div style="margin-top: 8px">上传图片</div>
    </div>
  </a-upload>
  <a-modal :open="previewVisible" :title="previewTitle" :footer="null" @cancel="handleCancel">
    <img alt="XuanChen" style="width: 100%" :src="previewImage" />
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { UploadFile, UploadChangeParam } from 'ant-design-vue';
import { useAuthStore } from '@/stores';
import { message } from 'ant-design-vue';
/** 定义变量 */
const authStore = useAuthStore();
const headers = reactive({ "XC-ACCESS-TOKEN": authStore.getToken(), });
const uploadUrl = import.meta.env.APP_FILE_UPLOAD_PATH;
const loading = ref(true);
const previewVisible = ref(false);
const previewImage = ref('');
const previewTitle = ref('');
const fileList = ref<UploadFile[]>([]);
// 定义 emits 用于向父组件传递更新后的 fileList
const emit = defineEmits(['update:fileList']);
/** 接收变量 */
const props = defineProps({
  imagePath: { type: String, default: '' },
  fileList: { type: Array, default: () => [] },
  listType: { type: String, default: 'picture-card' }, // 自定义展示类型
  maxCount: { type: Number, default: 5 } // 最大上传数量
})
/** 方法 */
function getBase64(file: File) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });
}

const beforeUpload = (file: any) => {
  // 检查数量限制
  if (fileList.value.length >= props.maxCount) {
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

const handleChange = (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    loading.value = true;
    return;
  }
  if (info.file.status === 'done') {
    loading.value = false;
    // 直接将完整的 fileList 传回给父组件
    emit('update:fileList', info.fileList);
  }
  if (info.file.status === 'error') {
    loading.value = false;
    message.error('上传失败');
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

watch(() => props.fileList, (newVal: any) => {
  if (newVal && Array.isArray(newVal)) {
    // 如果传入的是字符串数组，则转换为 UploadFile 格式
    if (newVal.length > 0 && typeof newVal[0] === 'string' && newVal[0] !== null) {
      fileList.value = newVal.map((url: string) => ({
        uid: url,
        name: url.substring(url.lastIndexOf('/') + 1),
        status: 'done',
        url: import.meta.env.APP_FILE_VIEW_PATH + url,
        response: { msg: url }
      }));
    } else {
      // 如果已经是 UploadFile 格式，直接使用
      fileList.value = [...newVal];
    }
  } else {
    fileList.value = [];
  }
}, { deep: true, immediate: true });
</script>