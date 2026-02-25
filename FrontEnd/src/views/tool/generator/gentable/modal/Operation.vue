<template>
  <a-modal v-model:open="visible" :title="operationTitle" width="100%" wrap-class-name="full-modal" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item name="tableName" label="表名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.tableName" placeholder="请输入表名" allowClear />
      </a-form-item>
      <a-form-item name="tableComment" label="表注释" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.tableComment" placeholder="请输入表注释" allowClear />
      </a-form-item>
      <a-form-item name="subTableName" label="子表名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.subTableName" placeholder="请输入子表名" allowClear />
      </a-form-item>
      <a-form-item name="subTableFkName" label="子表外键名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.subTableFkName" placeholder="请输入子表外键名" allowClear />
      </a-form-item>
      <a-form-item name="template" label="模板" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-select v-model:value="model.template" placeholder="请选择模板" allowClear>
          <a-select-option value="1">默认模板</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="outputDir" label="输出目录" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.outputDir" placeholder="请输入输出目录" allowClear />
      </a-form-item>
      <a-form-item name="packageName" label="包名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.packageName" placeholder="请输入包名" allowClear />
      </a-form-item>
      <a-form-item name="moduleName" label="模块名" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.moduleName" placeholder="请输入模块名" allowClear />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import type { GenTable } from '../gentable.types';
import { saveOrUpdate } from '../gentable.api';

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
const roleCodeDisabled = ref(false);

const model = reactive<GenTable>({
  id: '',
  tableName: '',
  tableComment: '',
  subTableName: '',
  subTableFkName: '',
  template: '',
  outputDir: '',
  packageName: '',
  moduleName: '',
})

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
  ],
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
  ],
}

//打开弹窗
const add = () => {
  visible.value = true;
  roleCodeDisabled.value = false;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.tableName = '';
  model.tableComment = '';
  model.subTableName = '';
  model.subTableFkName = '';
  model.template = '';
  model.outputDir = '';
  model.packageName = '';
  model.moduleName = '';
}
const edit = (records: any) => {
  visible.value = true;
  roleCodeDisabled.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.tableName = records.tableName;
  model.tableComment = records.tableComment;
  model.subTableName = records.subTableName;
  model.subTableFkName = records.subTableFkName;
  model.template = records.template;
  model.outputDir = records.outputDir;
  model.packageName = records.packageName;
  model.moduleName = records.moduleName;
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

<style scoped lang="scss">
.full-modal {
  .ant-modal {
    max-width: 100%;
    top: 0;
    padding-bottom: 0;
    margin: 0;
  }
  .ant-modal-content {
    display: flex;
    flex-direction: column;
    height: calc(100vh);
  }
  .ant-modal-body {
    flex: 1;
  }
}
</style>