<template>
  <a-modal v-model:open="visible" :title="operationTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item name="postCode" label="岗位编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.postCode" placeholder="请输入岗位编码" allowClear :disabled="postCodeDisabled" />
      </a-form-item>
      <a-form-item name="postName" label="岗位名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.postName" placeholder="请输入岗位名称" allowClear />
      </a-form-item>
      <a-form-item label="岗位描述" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.postDescription" placeholder="请输入岗位描述" allowClear />
      </a-form-item>
      <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-switch v-model:checked="model.status" checked-children="启用" un-checked-children="停用" />
      </a-form-item>
      <a-form-item label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-number v-model:value="model.orderNo" placeholder="请输入排序码" allowClear style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import { saveOrUpdate, validatePostCodeApi, validatePostNameApi } from '../post.api';
import type { PostModel } from '../post.types';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
})

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };

const emit = defineEmits(['childOK']);

const visible = ref(false);
const postCodeDisabled = ref(false);

const model = reactive<PostModel>({
  id: '',
  postCode: '',
  postName: '',
  postDescription: '',
  status: true,
  orderNo: '',
})

const validatePostCode = async (_rule: Rule, value: string) => {
  return await validatePostCodeApi(model.id, value);
}

const validatePostName = async (_rule: Rule, value: string) => {
  return await validatePostNameApi(model.id, value);
}

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  postCode: [
    { required: true, message: '请输入岗位编码', trigger: 'blur' },
    { required: true, validator: validatePostCode, trigger: 'blur' }
  ],
  postName: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { required: true, validator: validatePostName, trigger: 'blur' }
  ],
}

//打开弹窗
const add = () => {
  visible.value = true;
  postCodeDisabled.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.postCode = '';
  model.postName = '';
  model.postDescription = '';
  model.status = true;
  model.orderNo = '';
}
const edit = (records: PostModel) => {
  visible.value = true;
  postCodeDisabled.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.postCode = records.postCode;
  model.postName = records.postName;
  model.postDescription = records.postDescription;
  model.status = records.status;
  model.orderNo = records.orderNo;
}

const handleOk = async () => {
  await rulesRef.value.validate();
  const res: any = await saveOrUpdate(model);
  message.success(res.msg);
  emit('childOK');
  visible.value = false;
};


//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>