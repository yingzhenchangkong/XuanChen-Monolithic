<template>
  <a-row :gutter="20">
    <a-col :span="5">
      <a-card title="修改头像" style="height: 400px;">
        <a-upload v-model:file-list="fileList" name="file" list-type="picture-card" class="avatar-uploader"
          :show-upload-list="false" :action="url.upload" :headers="headers" :data="{ customPath: customPath }"
          :before-upload="beforeUpload" @change="handleChange">
          <a-avatar class="avatar" :src="imageUrl" :size="150">
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
        </a-upload>
      </a-card>
    </a-col>
    <a-col :span="10">
      <a-card title="修改资料" style="height: 400px;">
        <a-form layout="inline" :model="modelInfo" :rules="rulesInfo" ref="rulesInfoRef" class="modal-form-style">
          <a-form-item label="用户名" :labelCol="labelColA" :wrapperCol="wrapperColA">
            <a-input v-model:value="modelInfo.userName" disabled />
          </a-form-item>
          <a-form-item label="昵称" :labelCol="labelColA" :wrapperCol="wrapperColA" name="nickName">
            <a-input v-model:value="modelInfo.nickName" placeholder="请输入昵称" allowClear />
          </a-form-item>
          <a-form-item label="手机号" :labelCol="labelColA" :wrapperCol="wrapperColA" name="mobile">
            <a-input v-model:value="modelInfo.mobile" placeholder="请输入手机号" allowClear />
          </a-form-item>
          <a-form-item label="邮箱" :labelCol="labelColA" :wrapperCol="wrapperColA" name="email">
            <a-input v-model:value="modelInfo.email" placeholder="请输入邮箱" allowClear />
          </a-form-item>
          <a-form-item :wrapper-col="{ offset: 20, span: 2 }">
            <a-button @click="saveInfo" style="margin-left: 10px;" type="primary">保存</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </a-col>
    <a-col :span="9">
      <a-card title="修改密码" style="height: 400px;">
        <a-form layout="inline" :model="modelPWD" :rules="rulesPWD" ref="rulesPWDRef" class="modal-form-style">
          <a-form-item label="新密码" :labelCol="labelColB" :wrapperCol="wrapperColB" name="password">
            <a-input-password v-model:value="modelPWD.password" placeholder="请输入新密码" allowClear autocomplete="off" />
          </a-form-item>
          <a-form-item label="确认密码" :labelCol="labelColB" :wrapperCol="wrapperColB" name="confirmPassword">
            <a-input-password v-model:value="modelPWD.confirmPassword" placeholder="请再次输入新密码" allowClear
              autocomplete="off" />
          </a-form-item>
          <a-form-item :wrapper-col="{ offset: 17, span: 5 }">
            <a-button @click="savePWD" style="margin-left: 10px;" type="primary">保存</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </a-col>
  </a-row>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '@/stores';
import { getAction, postAction, httpAction } from '@/utils/httpAction';
import { useRouter } from 'vue-router';
import type { UploadChangeParam } from 'ant-design-vue';
import { onMounted } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';

const router = useRouter();
const userStore = useAuthStore();

const labelColA = { span: 4 };
const wrapperColA = { span: 20 };

const labelColB = { span: 4 };
const wrapperColB = { span: 18 };

const userInfo = userStore.getUserInfo();
const headers = reactive({
  "XC-ACCESS-TOKEN": userStore.getToken()
})
const url = {
  changePassword: '/system/user/changePassword',
  edit: '/system/user/userCenterEdit',
  upload: import.meta.env.APP_FILE_UPLOAD_PATH,
  updateAvatar: '/system/user/userCenterUpdateAvatar',
  validateMobile: '/system/user/validateMobile',
  validateEmail: '/system/user/validateEmail',
}

const customPath = "avatar";

onMounted(() => {
  imageUrl.value = import.meta.env.APP_FILE_VIEW_PATH + userInfo.avatar
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

const handleChange = (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    loading.value = true;
    return;
  }
  if (info.file.status === 'done') {
    let model = {
      avatar: fileList.value[0].response.msg,
      userName: userInfo.userName,
    }
    httpAction(url.updateAvatar, model, 'put').then((res: any) => {
      message.success(res.msg);
      userInfo.avatar = model.avatar;
    })

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
const modelInfo = reactive({
  id: userInfo.id,
  userName: userInfo.userName,
  nickName: userInfo.nickName,
  mobile: userInfo.mobile,
  email: userInfo.email,
})
const validateMobile = async (_rule: Rule, value: string) => {
  const res: any = await getAction(url.validateMobile, { id: modelInfo.id, mobile: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}
const validateEmail = async (_rule: Rule, value: string) => {
  const res: any = await getAction(url.validateEmail, { id: modelInfo.id, email: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}
const rulesInfoRef = ref()
const rulesInfo: Record<string, Rule[]> = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3|4|5|6|7|8|9][0-9]{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    { required: true, validator: validateMobile, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '请输入正确的邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
}
const saveInfo = () => {
  rulesInfoRef.value.validate().then(() => {
    postAction(url.edit, modelInfo).then((res: any) => {
      if (res.code === 200) {
        message.success(res.msg);
      }
    })
  }).catch(() => {
    message.error('输入有误，请重新输入');
  })
}

const modelPWD = reactive({
  password: '',
  confirmPassword: '',
})
const validateConfirmPassword = (_rule: Rule, value: string) => {
  if (value !== modelPWD.password) {
    return Promise.reject('两次输入的密码不一致！');
  } else {
    return Promise.resolve();
  }
}
const rulesPWDRef = ref()
const rulesPWD: Record<string, Rule[]> = {
  password: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, trigger: 'blur' }
  ],
  confirmPassword: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' },
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
}

const savePWD = () => {
  rulesPWDRef.value.validate().then(() => {
    postAction(url.changePassword, { password: modelPWD.password }).then((resChangePassword: any) => {
      if (resChangePassword.code === 200) {
        message.success('修改成功');
        getAction('/logout', {}).then(resLogout => {
          window.sessionStorage.clear();
          router.push("/login");
        })
      }

    })
  }).catch(() => {
    message.error('输入有误，请重新输入');
  })
}
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