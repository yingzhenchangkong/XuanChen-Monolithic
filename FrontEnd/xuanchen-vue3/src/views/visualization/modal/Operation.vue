<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="640" :show-fullscreen-btn="false" @ok="handleOk">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
      <a-row style="width: 100%">
        <a-col :span="12">
          <a-form-item name="screenCode" label="大屏编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.screenCode" placeholder="请输入大屏编码" allowClear :maxlength="64"
              :disabled="isEdit" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="screenName" label="大屏名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.screenName" placeholder="请输入大屏名称" allowClear :maxlength="100" />
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
      <a-row style="width: 100%">
        <a-col :span="24">
          <a-form-item label="缩略图" :labelCol="{ span: 4 }" :wrapperCol="{ span: 20 }">
            <XCUploadImage v-model:fileList="thumbList" image-path="visualization" :max-count="1" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div class="code-tip">
      提示：大屏编码创建后不可修改，展示页通过编码访问（/visualization/viewer/编码）。
      画布与组件配置在「大屏设计」中编辑并保存；缩略图仅用于列表/设计画廊展示。
    </div>
  </XCModal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';

import XCModal from '@/components/xuanchen/XCModal.vue';
import XCUploadImage from '@/components/xuanchen/XCUploadImage.vue';
import { saveOrUpdateScreen } from '../visualization.api';
import type { VisualizationRecord } from '../visualization.types';

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
  screenCode: [
    { required: true, message: '请输入大屏编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '编码以字母开头，仅含字母/数字/下划线', trigger: 'blur' },
  ],
  screenName: [{ required: true, message: '请输入大屏名称', trigger: 'blur' }],
};

const visible = ref(false);
const model = reactive({
  id: '' as string,
  screenCode: '',
  screenName: '',
  remark: '',
  thumbnail: '',
});

const isEdit = computed(() => !!model.id);

/** XCUploadImage 的 v-model 是字符串数组，表单单值字段用 computed 桥接 */
const thumbList = computed<string[]>({
  get: () => (model.thumbnail ? [model.thumbnail] : []),
  set: (list: string[]) => {
    model.thumbnail = list[0] ?? '';
  },
});

const add = () => {
  visible.value = true;
  model.id = '';
  model.screenCode = '';
  model.screenName = '';
  model.remark = '';
  model.thumbnail = '';
  rulesRef.value?.clearValidate();
};

const edit = (record: VisualizationRecord) => {
  visible.value = true;
  model.id = record?.id ? String(record.id) : '';
  model.screenCode = record?.screenCode ? String(record.screenCode) : '';
  model.screenName = record?.screenName ? String(record.screenName) : '';
  model.remark = record?.remark ? String(record.remark) : '';
  model.thumbnail = record?.thumbnail ? String(record.thumbnail) : '';
  rulesRef.value?.clearValidate();
};

const handleOk = async () => {
  await rulesRef.value?.validate();
  const res = await saveOrUpdateScreen({
    id: model.id || undefined,
    screenCode: model.screenCode.trim(),
    screenName: model.screenName.trim(),
    remark: model.remark,
    thumbnail: model.thumbnail,
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
  line-height: 1.7;
}
</style>
