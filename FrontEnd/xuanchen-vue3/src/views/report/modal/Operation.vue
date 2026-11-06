<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="640" :show-fullscreen-btn="false" @ok="handleOk">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-row style="width: 100%">
        <a-col :span="12">
          <a-form-item name="reportCode" label="报表编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.reportCode" placeholder="请输入报表编码" allowClear :maxlength="64"
              :disabled="isEdit" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="reportName" label="报表名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.reportName" placeholder="请输入报表名称" allowClear :maxlength="100" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row style="width: 100%">
        <a-col :span="12">
          <a-form-item name="reportType" label="报表类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-model:value="model.reportType" style="width: 100%" :options="reportTypeOptions"
              placeholder="请选择报表类型" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="templateId" label="模板" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <!-- 模板来源「报表管理 - 模板设计」；仅新增时可选，创建时复制模板版式，之后与模板解耦 -->
            <a-select v-model:value="model.templateId" style="width: 100%" :options="templateOptions"
              :loading="templateLoading" placeholder="请选择模板" show-search option-filter-prop="label"
              :disabled="isEdit" allow-clear />
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
    <div class="code-tip">
      提示：报表编码创建后不可修改，查看页通过编码访问（/report/viewer/编码）。
      模板决定版式、数据集与字段绑定（在「模板设计 → 设计」中维护），创建报表时复制模板设计，之后与模板解耦。
    </div>
  </XCModal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';

import XCModal from '@/components/xuanchen/XCModal.vue';
import { saveOrUpdateReport } from '../report.api';
import { getAllTemplatesApi } from '../template/template.api';
import { reportTypeOptions } from '../report.data';
import type { ReportRecord } from '../report.types';
import type { TemplateRecord } from '../template/template.types';

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
  reportCode: [
    { required: true, message: '请输入报表编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码以字母开头，仅含字母/数字/下划线', trigger: 'blur' },
  ],
  reportName: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  reportType: [{ required: true, message: '请选择报表类型', trigger: 'change' }],
};

const visible = ref(false);
const model = reactive({
  id: '' as string,
  reportCode: '',
  reportName: '',
  reportType: 1 as number,
  templateId: undefined as string | undefined,
  remark: '',
});

const isEdit = computed(() => !!model.id);

/** 模板下拉（来源「模板设计」，仅启用中的模板） */
const templateOptions = ref<{ label: string; value: string }[]>([]);
const templateLoading = ref(false);
const loadTemplates = async () => {
  templateLoading.value = true;
  try {
    const list = await getAllTemplatesApi();
    templateOptions.value = (list as TemplateRecord[]).map((item) => ({
      label: `${item.templateName ?? ''} (${item.templateCode ?? ''})`,
      value: String(item.id),
    }));
  } catch {
    templateOptions.value = [];
    message.warning('模板列表加载失败，请检查「模板设计」是否已配置模板');
  } finally {
    templateLoading.value = false;
  }
};

//打开弹窗
const add = () => {
  visible.value = true;
  model.id = '';
  model.reportCode = '';
  model.reportName = '';
  model.reportType = 1;
  model.templateId = undefined;
  model.remark = '';
  rulesRef.value?.clearValidate();
  loadTemplates();
};
const edit = (record: ReportRecord) => {
  visible.value = true;
  model.id = record?.id ? String(record.id) : '';
  model.reportCode = record?.reportCode ? String(record.reportCode) : '';
  model.reportName = record?.reportName ? String(record.reportName) : '';
  model.reportType = record?.reportType != null ? Number(record.reportType) : 1;
  model.templateId = record?.templateId ? String(record.templateId) : undefined;
  model.remark = record?.remark ? String(record.remark) : '';
  rulesRef.value?.clearValidate();
  loadTemplates();
};

const handleOk = async () => {
  await rulesRef.value?.validate();
  if (!isEdit.value && !model.templateId) {
    message.warning('请选择报表模板！');
    return Promise.reject();
  }
  const res = await saveOrUpdateReport({
    id: model.id || undefined,
    reportCode: model.reportCode.trim(),
    reportName: model.reportName.trim(),
    reportType: model.reportType,
    templateId: model.templateId,
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
