<template>
  <div class="bpm-apply-page">
    <!-- 左侧：可发起的流程列表 -->
    <div class="bpm-apply-side">
      <h4>可发起的流程</h4>
      <div v-for="item in definitions" :key="item.id" class="bpm-apply-def"
        :class="{ active: item.id === selected?.id }" @click="handleSelect(item)">
        <div class="bpm-apply-def-name">{{ item.name }}</div>
        <div class="bpm-apply-def-code">{{ item.code }}</div>
      </div>
      <a-empty v-if="!loading && !definitions.length" description="暂无可发起的流程（请先在流程配置中启用）" />
    </div>

    <!-- 右侧：申请表单 -->
    <div class="bpm-apply-main">
      <template v-if="selected">
        <a-form layout="vertical" class="bpm-apply-form">
          <a-form-item label="申请摘要" required>
            <a-input v-model:value="form.summary" placeholder="请输入申请摘要（将展示在审批人的待办中）"
              :maxlength="255" allowClear />
          </a-form-item>
          <a-form-item label="申请内容（条件表达式的变量来源）">
            <!-- 绑定了在线表单：渲染设计器产出的字段 -->
            <template v-if="formSchema">
              <XCFormRender ref="formRenderRef" :schema="formSchema" :model-value="formModel" />
            </template>
            <!-- 未绑定在线表单：通用 key/value 字段 -->
            <template v-else>
              <div v-for="(field, idx) in form.fields" :key="idx" class="bpm-apply-field-row">
                <a-input v-model:value="field.key" placeholder="字段名（如 amount）" style="width: 220px" />
                <a-input v-model:value="field.value" placeholder="字段值" style="flex: 1" />
                <a @click="form.fields.splice(idx, 1)">
                  <MinusCircleOutlined />
                </a>
              </div>
              <a @click="form.fields.push({ key: '', value: '' })">
                <PlusCircleOutlined /> 添加字段
              </a>
            </template>
          </a-form-item>
          <!-- 需要发起人自选审批人的节点 -->
          <a-form-item v-for="node in chooserNodes" :key="node.key" :label="`选择「${node.name || node.key}」的审批人`" required>
            <a-select v-model:value="chosen[node.key]" mode="multiple" placeholder="请选择审批人（可多选）"
              :options="userOptions" allowClear />
          </a-form-item>
        </a-form>
        <a-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
          <SendOutlined /> 提交申请
        </a-button>
      </template>
      <a-empty v-else description="请选择左侧要发起的流程" style="margin-top: 80px" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import {
  MinusCircleOutlined, PlusCircleOutlined, SendOutlined,
} from '@ant-design/icons-vue';

import { getAction } from '@/utils/httpAction';
import XCFormRender from '@/components/xuanchen/XCFormRender.vue';
import { getDefinitionListApi } from '../definition/definition.api';
import { getFormDetailApi } from '../form/form.api';
import { getUserOptionsApi, submitInstanceApi } from '../businessprocess.api';
import type { BpmUserOption } from '../businessprocess.types';
import type { BpmDefinitionRecord } from '../definition/definition.types';

const loading = ref(false);
const submitting = ref(false);
const definitions = ref<BpmDefinitionRecord[]>([]);
const selected = ref<BpmDefinitionRecord | null>(null);
const userOptions = ref<BpmUserOption[]>([]);
/** 节点规则（需要发起人自选的节点） */
const chooserNodes = ref<Array<{ key: string; name: string }>>([]);

const form = reactive({
  summary: '',
  fields: [{ key: '', value: '' }] as Array<{ key: string; value: string }>,
});
const chosen = reactive<Record<string, string[]>>({});

/** 当前流程绑定的在线表单设计（空=使用通用字段表单） */
const formSchema = ref<string | null>(null);
/** 在线表单数据对象（XCFormRender 就地写入） */
const formModel = reactive<Record<string, unknown>>({});
const formRenderRef = ref<InstanceType<typeof XCFormRender>>();

const canSubmit = computed(() => !!selected.value && !!form.summary.trim());

const handleSelect = async (item: BpmDefinitionRecord) => {
  selected.value = item;
  form.summary = '';
  form.fields = [{ key: '', value: '' }];
  Object.keys(chosen).forEach((k) => delete chosen[k]);
  Object.keys(formModel).forEach((k) => delete formModel[k]);
  formSchema.value = null;
  chooserNodes.value = [];
  // 拉取该流程需要"发起人自选"的节点
  await loadDefinitionRules(item.id);
  // 加载绑定的在线表单设计（失败/未绑定都回落通用字段表单）
  if (item.onlineFormId) {
    try {
      const res = await getFormDetailApi(item.onlineFormId);
      if (res.code === 200 && res.data?.schemaJson) {
        formSchema.value = res.data.schemaJson;
      }
    } catch {
      /* 表单加载失败回落通用字段表单 */
    }
  }
};

const loadDefinitionRules = async (definitionId?: string) => {
  if (!definitionId) return;
  try {
    const res = await getAction<Array<{ key: string; name: string }>>(
      '/businessprocess/common/chooserNodes', { definitionId });
    if (res.code !== 200) return;
    chooserNodes.value = res.data ?? [];
    chooserNodes.value.forEach((n) => {
      chosen[n.key] = [];
    });
  } catch {
    /* 规则加载失败不阻塞表单 */
  }
};

const handleSubmit = async () => {
  if (!selected.value?.id) return;
  if (!form.summary.trim()) {
    message.warning('请填写申请摘要');
    return;
  }
  // 自选审批人校验
  for (const node of chooserNodes.value) {
    if (!chosen[node.key]?.length) {
      message.warning(`请为「${node.name || node.key}」选择审批人`);
      return;
    }
  }
  // 组装 formData：绑定了在线表单走渲染器收集+必填校验，否则读通用字段
  let formData: Record<string, unknown>;
  if (formSchema.value) {
    const missLabel = formRenderRef.value?.validate() ?? null;
    if (missLabel) {
      message.warning(`「${missLabel}」为必填项`);
      return;
    }
    formData = formRenderRef.value?.collect() ?? {};
  } else {
    formData = {};
    form.fields.forEach((f) => {
      const key = f.key.trim();
      if (key) formData[key] = f.value;
    });
  }
  submitting.value = true;
  try {
    const res = await submitInstanceApi({
      definitionId: selected.value.id,
      businessId: `apply_${Date.now()}`,
      businessSummary: form.summary.trim(),
      formData,
      chosenAssignees: Object.fromEntries(Object.entries(chosen).map(([k, v]) => [k, v ?? []])),
    });
    if (res.code !== 200) {
      message.error(res.msg || '提交失败');
      return;
    }
    message.success('提交成功！可在「我的申请」中查看进度。');
    form.summary = '';
    form.fields = [{ key: '', value: '' }];
    Object.keys(formModel).forEach((k) => delete formModel[k]);
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  loading.value = true;
  try {
    const res = await getDefinitionListApi({ pageNo: 1, pageSize: 100, status: 1 });
    definitions.value = res.data?.records ?? [];
    if (definitions.value.length) {
      handleSelect(definitions.value[0]);
    }
  } finally {
    loading.value = false;
  }
  try {
    userOptions.value = await getUserOptionsApi();
  } catch {
    message.error('用户列表加载失败，"发起人自选"节点将无法选择审批人');
  }
});
</script>

<style scoped>
.bpm-apply-page {
  display: flex;
  gap: 16px;
  min-height: calc(100vh - 160px);
}

.bpm-apply-side {
  width: 260px;
  background: #fff;
  border-radius: 6px;
  padding: 16px;
}

.bpm-apply-side h4 {
  font-size: 13px;
  margin-bottom: 12px;
}

.bpm-apply-def {
  padding: 10px 12px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.bpm-apply-def:hover {
  background: #f5f5f5;
}

.bpm-apply-def.active {
  background: #e6f7ff;
  border-color: #91caff;
}

.bpm-apply-def-name {
  font-weight: 500;
}

.bpm-apply-def-code {
  font-size: 12px;
  color: #999;
}

.bpm-apply-main {
  flex: 1;
  background: #fff;
  border-radius: 6px;
  padding: 16px;
}

.bpm-apply-form {
  max-width: 680px;
}

.bpm-apply-field-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
