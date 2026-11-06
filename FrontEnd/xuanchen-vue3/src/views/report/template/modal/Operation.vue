<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="640" :show-fullscreen-btn="false" @ok="handleOk">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-row style="width: 100%">
        <a-col :span="12">
          <a-form-item name="templateCode" label="模板编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.templateCode" placeholder="请输入模板编码" allowClear :maxlength="64"
              :disabled="isEdit" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="templateName" label="模板名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.templateName" placeholder="请输入模板名称" allowClear :maxlength="100" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row style="width: 100%">
        <a-col :span="12">
          <a-form-item name="templateType" label="模板类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-model:value="model.templateType" style="width: 100%" :options="reportTypeOptions"
              placeholder="请选择模板类型" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="status" label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-model:value="model.status" style="width: 100%" :options="templateStatusOptions"
              placeholder="请选择状态" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row style="width: 100%">
        <a-col :span="24">
          <a-form-item name="remark" label="备注" :labelCol="{ span: 4 }" :wrapperCol="{ span: 20 }">
            <a-input v-model:value="model.remark" placeholder="请输入备注" allowClear :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div class="code-tip">提示：模板编码创建后不可修改；停用的模板不会再出现在报表新增的「模板」下拉中。</div>
  </XCModal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';

import XCModal from '@/components/xuanchen/XCModal.vue';
import { saveOrUpdateTemplate } from '../template.api';
import { templateStatusOptions } from '../template.data';
import { reportTypeOptions } from '../../report.data';
import type { TemplateRecord } from '../template.types';

const labelCol = { span: 7 };
const wrapperCol = { span: 17 };

defineProps({
  operationTitle: {
    type: String,
    default: '编辑',
  },
});
const emit = defineEmits(['childOK']);

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  templateCode: [
    { required: true, message: '请输入模板编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码以字母开头，仅含字母/数字/下划线', trigger: 'blur' },
  ],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
};

const visible = ref(false);
const model = reactive({
  id: '' as string,
  templateCode: '',
  templateName: '',
  templateType: 1 as number,
  status: 1 as number,
  remark: '',
});

const isEdit = computed(() => !!model.id);

//打开弹窗
const add = () => {
  visible.value = true;
  model.id = '';
  model.templateCode = '';
  model.templateName = '';
  model.templateType = 1;
  model.status = 1;
  model.remark = '';
  rulesRef.value?.clearValidate();
};
const edit = (record: TemplateRecord) => {
  visible.value = true;
  model.id = record?.id ? String(record.id) : '';
  model.templateCode = record?.templateCode ? String(record.templateCode) : '';
  model.templateName = record?.templateName ? String(record.templateName) : '';
  model.templateType = record?.templateType != null ? Number(record.templateType) : 1;
  model.status = record?.status != null ? Number(record.status) : 1;
  model.remark = record?.remark ? String(record.remark) : '';
  rulesRef.value?.clearValidate();
};

const handleOk = async () => {
  await rulesRef.value?.validate();
  const res = await saveOrUpdateTemplate({
    id: model.id || undefined,
    templateCode: model.templateCode.trim(),
    templateName: model.templateName.trim(),
    templateType: model.templateType,
    status: model.status,
    remark: model.remark,
  });
  if (res.code !== 200) {
    message.error(res.msg || '保存失败');
    return Promise.reject();
  }
  message.success('保存成功！');
  visible.value = false;
  emit('childOK');
};

defineExpose({
  add,
  edit,
});
</script>

<style scoped>
.code-tip {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.65;
}
</style>
