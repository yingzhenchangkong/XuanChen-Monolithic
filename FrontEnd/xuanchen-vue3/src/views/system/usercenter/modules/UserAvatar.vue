<template>
  <a-card title="修改头像" style="height: 400px;">
    <a-upload v-model:file-list="fileList" name="file" list-type="picture-card" class="avatar-uploader"
      :show-upload-list="false" :action="action" :headers="headers" :data="{ customPath: customPath }"
      :before-upload="beforeUpload" @change="handleChange">
      <a-avatar class="avatar" :src="imageUrl" :size="150">
        <template #icon>
          <UserOutlined />
        </template>
      </a-avatar>
    </a-upload>
  </a-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import type { UploadChangeParam } from 'ant-design-vue';
import { useAuthStore } from '@/stores';
import { userCenterUpdateAvatar } from '../../user/user.api';

const userStore = useAuthStore();
const userInfo = userStore.getUserInfo();
const headers = reactive({
  "XC-ACCESS-TOKEN": userStore.getToken()
})

const action = import.meta.env.APP_FILE_UPLOAD_PATH;
const customPath = "avatar";

onMounted(() => {
  imageUrl.value = import.meta.env.APP_FILE_VIEW_PATH + userInfo.avatar;
})

/** 上传开始 */
function getBase64(img: any, callback: (base64Url: string) => void) {
  const reader = new FileReader();
  reader.addEventListener('load', () => callback(reader.result as string));
  reader.readAsDataURL(img);
}

const fileList = ref([] as any[]);
const loading = ref<boolean>(false);
const imageUrl = ref<string>('');

const handleChange = async (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    loading.value = true;
    return;
  }
  if (info.file.status === 'done') {
    const res: any = await userCenterUpdateAvatar(userInfo.userName, fileList.value[0].response.msg);
    message.success(res.msg);
    userInfo.avatar = fileList.value[0].response.msg;
    userStore.setUserInfo(userInfo);

    getBase64(info.file.originFileObj, (base64Url: string) => {
      imageUrl.value = base64Url;
      loading.value = false;
    });
  }
  if (info.file.status === 'error') {
    loading.value = false;
    message.error('上传失败');
  }
};

const beforeUpload = (file: any) => {
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
/** 上传结束 */
</script>
<style scoped>
.avatar {
  position: relative;
  cursor: pointer;
}

.avatar:hover::after {
  content: "+";
  font-size: 32px;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.3);
  position: absolute;
  top: 0;
  left: 0;
}

:deep(.ant-upload-wrapper.ant-upload-picture-card-wrapper .ant-upload.ant-upload-select) {
  width: 150px;
  height: 150px;
  border-radius: 50%;
}
</style>