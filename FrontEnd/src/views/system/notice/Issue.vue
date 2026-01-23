<template>
  <a-card>
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item name="title" label="通知标题" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.title" placeholder="请输入通知标题" allowClear />
      </a-form-item>
      <a-form-item name="listUser" label="接收人" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.listUser" style="width: 100%" :options="optionsUser" mode="multiple"
          :fieldNames="{ label: 'nickName', value: 'userName' }" placeholder="请选择接收人" allowClear>
        </a-select>
      </a-form-item>
      <a-form-item name="content" label="通知内容" :labelCol="labelCol" :wrapperCol="wrapperCol" class="editor-wrapper">
        <QuillEditor v-model:content="model.content" :options="editorOptions" content-type="html" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="issueSubmit" style="width: 66px;float: right;">发布</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import { getUserSelect } from '../user/user.api';

// Quill 局部导入组件和样式
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { issueApi } from './notice.api';

// Quill 编辑器配置项
const editorOptions = {
  theme: 'snow', // 使用 snow 主题
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'], // 加粗、斜体、下划线、删除线
      ['blockquote', 'code-block'], // 引用、代码块
      [{ 'header': [1, 2, 3, 4, 5, 6, false] }], // 标题级别
      [{ 'size': ['small', false, 'large', 'huge'] }], // 字体大小
      [{ 'color': [] }, { 'background': [] }], // 文字颜色、背景色
      [{ 'font': [] }], // 字体
      [{ 'script': 'sub' }, { 'script': 'super' }], // 下标、上标
      [{ 'align': [] }], // 对齐方式
      [{ 'list': 'ordered' }, { 'list': 'bullet' }], // 有序列表、无序列表
      [{ 'indent': '-1' }, { 'indent': '+1' }], // 缩进
      ['link', 'image'], // 链接、图片
      ['clean'] // 清除格式
    ]
  },
  placeholder: '请输入通知内容...',
}

const labelCol = { span: 2 };
const wrapperCol = { span: 22 };

const model = reactive({
  title: '',
  content: '',
  listUser: [],
})

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  title: [
    { required: true, message: '请输入通知标题', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入通知内容', trigger: 'blur' },
  ],
  listUser: [
    { required: true, message: '请选择接收人', trigger: 'blur' },
  ],
}

const optionsUser = ref([]);
const getSelectUser = async () => {
  optionsUser.value = await getUserSelect();
};
getSelectUser();

const issueSubmit = async () => {
  await rulesRef.value.validate();
  const res: any = await issueApi(model);
  message.success(res.msg);
};
</script>

<style scoped>
:deep(.editor-wrapper .ql-editor) {
  min-height: 500px;
}
</style>