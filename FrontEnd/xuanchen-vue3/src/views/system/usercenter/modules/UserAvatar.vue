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
import type { UploadChangeParam, UploadFile } from 'ant-design-vue';
import { useAuthStore } from '@/stores';
import { userCenterUpdateAvatar } from '../../user/user.api';
import { getImageView } from '@/utils/ImageUtil';

const userStore = useAuthStore();
// 仅在 setup 取一次用户名（上传接口入参，页面生命周期内不变）；头像等其他字段通过 store 实时读
const userName = userStore.getUserInfo()?.userName ?? '';
const headers = reactive<Record<string, string>>({
  "XC-ACCESS-TOKEN": userStore.getToken() ?? ''
})

// a-upload 的 action 是组件内部直发 XHR，不经过 axios 实例，需显式补 APP_BASE_URL 前缀（/api）
const action = import.meta.env.APP_BASE_URL + import.meta.env.APP_FILE_UPLOAD_PATH;
const customPath = "avatar";

onMounted(() => {
  // 统一走 getImageView：补 /api 前缀并对中文/空格文件名做编码
  imageUrl.value = getImageView(userStore.getUserInfo()?.avatar) ?? '';
})

/** 上传开始 */
function getBase64(img: File, callback: (base64Url: string) => void) {
  const reader = new FileReader();
  reader.addEventListener('load', () => callback(reader.result as string));
  reader.readAsDataURL(img);
}

const fileList = ref<UploadFile[]>([]);
const loading = ref<boolean>(false);
const imageUrl = ref<string>('');

const handleChange = async (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    loading.value = true;
    return;
  }
  if (info.file.status === 'done') {
    // 上传响应体为统一 Result，文件相对路径当前落在 msg 字段（见 XCUploadImage 同款处理）
    const avatarPath = fileList.value[0]?.response?.msg as string | undefined;
    if (!avatarPath) {
      loading.value = false;
      message.error('上传失败：未获取到文件路径');
      return;
    }
    try {
      const res = await userCenterUpdateAvatar(userName, avatarPath);
      if (res.code !== 200) {
        loading.value = false;
        message.error(res.msg);
        return;
      }
      message.success(res.msg);
      // 整体替换 store 中的用户对象，依赖头像的响应式消费者（如顶栏头像）才能收到更新
      const current = userStore.getUserInfo();
      if (current) {
        userStore.setUserInfo({ ...current, avatar: avatarPath });
      }

      getBase64(info.file.originFileObj as File, (base64Url: string) => {
        imageUrl.value = base64Url;
        loading.value = false;
      });
    } catch {
      // 网络/HTTP 错误已由响应拦截器统一提示
      loading.value = false;
    }
  }
  if (info.file.status === 'error') {
    loading.value = false;
    message.error('上传失败');
  }
};

const beforeUpload = (file: File) => {
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