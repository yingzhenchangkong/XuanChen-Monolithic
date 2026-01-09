<template>
  <a-modal v-model:open="visible" :title="modalTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-form-item name="userName" label="用户名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.userName" placeholder="请输入用户名" allowClear :disabled="userNameDisabled" />
      </a-form-item>
      <a-form-item name="nickName" label="昵称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.nickName" placeholder="请输入昵称" allowClear />
      </a-form-item>
      <a-form-item name="mobile" label="手机号" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.mobile" placeholder="请输入手机号" allowClear />
      </a-form-item>
      <a-form-item name="email" label="邮箱" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.email" placeholder="请输入邮箱" allowClear />
      </a-form-item>
      <a-form-item name="password" label="密码" :labelCol="labelCol" :wrapperCol="wrapperCol" v-if="passwordVisible">
        <a-input-password v-model:value="model.password" placeholder="无输入时自动设置:XuanChen@888888" allowClear
          autocomplete="off" />
      </a-form-item>
      <a-form-item name="deptIds" label="部门" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-tree-select v-model:value="model.deptIds" show-search style="width: 100%"
          :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" placeholder="请选择部门" allow-clear
          tree-default-expand-all :tree-data="treeData" tree-node-filter-prop="label" multiple
          :field-names="{ children: 'children', label: 'title', value: 'key', }">
        </a-tree-select>
      </a-form-item>
      <a-form-item name="roleIds" label="角色" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.roleIds" mode="multiple" style="width: 100%" :options="optionsRoleIds"
          :fieldNames="{ label: 'roleName', value: 'id' }" placeholder="请选择角色" :max-tag-count="maxTagCount" allowClear>
          <template #maxTagPlaceholder="omittedValues">
            <span style="color: red">+ {{ omittedValues.length }} ...</span>
          </template>
        </a-select>
      </a-form-item>
      <a-form-item name="avatar" label="头像" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-upload v-model:file-list="fileList" name="file" list-type="picture-card" class="avatar-uploader"
          :show-upload-list="false" :action="url.upload" :headers="headers" :data="{ customPath: customPath }"
          :before-upload="beforeUpload" @change="handleChange">
          <img v-if="model.avatar" :src="imageUrl" alt="avatar" width="100px" height="100px" />
          <div v-else>
            <loading-outlined v-if="loading"></loading-outlined>
            <plus-outlined v-else></plus-outlined>
            <div class="ant-upload-text">上传头像</div>
          </div>
        </a-upload>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import type { UploadChangeParam } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { getAction, httpAction } from '@/utils/httpAction';
import { useAuthStore } from '@/stores';

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const authStore = useAuthStore();
const headers = reactive({
  "XC-ACCESS-TOKEN": authStore.getToken(),
})
defineProps({
  modalTitle: {
    type: String,
    default: '编辑'
  }
})
const emit = defineEmits(['childOK']);
const url = {
  add: '/system/user/add',
  edit: '/system/user/edit',
  selectDept: '/system/dept/getDeptTree',
  selectRole: '/system/role/select',
  upload: import.meta.env.APP_FILE_UPLOAD_PATH,
  validateUserName: '/system/user/validateUserName',
  validateMobile: '/system/user/validateMobile',
  validateEmail: '/system/user/validateEmail',
}
const validateUserName = async (rule: Rule, value: string) => {
  const res: any = await getAction(url.validateUserName, { id: model.id, userName: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}
const validateMobile = async (rule: Rule, value: string) => {
  const res: any = await getAction(url.validateMobile, { id: model.id, mobile: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}
const validateEmail = async (rule: Rule, value: string) => {
  const res: any = await getAction(url.validateEmail, { id: model.id, email: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur' },
    { required: true, validator: validateUserName, trigger: 'blur' }
  ],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3|4|5|6|7|8|9][0-9]{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    { required: true, validator: validateMobile, trigger: 'blur' }
  ],
  email: [
    { pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '请输入正确的邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  password: [
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{6,20}$/, message: '密码由6-20位数字、大小写字母和特殊符号组成!', trigger: 'blur' }
  ],
}

const customPath = "avatar";

const visible = ref(false);
const userNameDisabled = ref(false);
const passwordVisible = ref(true);

const model = reactive({
  id: '',
  userName: '',
  nickName: '',
  mobile: '',
  email: '',
  password: undefined,
  roleIds: undefined,
  deptIds: undefined,
  avatar: undefined,
})

const maxTagCount = ref(3);
const optionsRoleIds = ref([])

const getSelectRole = async () => {
  const res = await getAction(url.selectRole, {});
  optionsRoleIds.value = res.data;
}

const treeData = ref();
const getDeptTree = async () => {
  treeData.value = await getAction(url.selectDept, {});
}
getDeptTree();

//打开弹窗
const add = () => {
  visible.value = true;
  userNameDisabled.value = false;
  passwordVisible.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.userName = '';
  model.nickName = '';
  model.mobile = '';
  model.email = '';
  model.password = undefined;
  model.roleIds = undefined;
  model.avatar = undefined;
  fileList.value = [];
}
const edit = (records: any) => {
  visible.value = true;
  userNameDisabled.value = true;
  passwordVisible.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.userName = records.userName;
  model.nickName = records.nickName;
  model.mobile = records.mobile;
  model.email = records.email;
  model.roleIds = records.roleIds;
  if (model.roleIds == null) {
    model.roleIds = undefined;
  }
  model.deptIds = records.deptIds;
  if (model.deptIds == null) {
    model.deptIds = undefined;
  }
  model.avatar = records.avatar;
  imageUrl.value = import.meta.env.APP_FILE_VIEW_PATH + records.avatar;
  fileList.value = [];
}
/** 上传开始 */
function getBase64(img: any, callback: (base64Url: string) => void) {
  const reader = new FileReader();
  reader.addEventListener('load', () => callback(reader.result as string));
  reader.readAsDataURL(img);
}

const fileList = ref();
const loading = ref<boolean>(false);
const imageUrl = ref<string>('');

const handleChange = (info: UploadChangeParam) => {
  if (info.file.status === 'uploading') {
    loading.value = true;
    return;
  }
  if (info.file.status === 'done') {
    getBase64(info.file.originFileObj, (base64Url: string) => {
      imageUrl.value = base64Url;
      loading.value = false;
      if (fileList.value && fileList.value.length > 0) {
        model.avatar = fileList.value[0].response.msg;
      }
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
const handleOk = () => {
  rulesRef.value.validate().then(() => {
    let httpurl = '';
    let method = '';
    if (model.id === '') {
      httpurl += url.add;
      method = 'post';
    } else {
      httpurl += url.edit;
      method = 'put';
    }
    httpAction(httpurl, model, method).then((res: any) => {
      message.success(res.msg);
      emit('childOK');
    })
    visible.value = false;
  }).catch(() => {
    message.error('输入有误，请重新输入');
  })
};

//子组件方法默认为私有
defineExpose({
  add,
  edit
})

getSelectRole()
</script>

<style scoped>
.avatar-uploader>.ant-upload {
  width: 128px;
  height: 128px;
}

.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>