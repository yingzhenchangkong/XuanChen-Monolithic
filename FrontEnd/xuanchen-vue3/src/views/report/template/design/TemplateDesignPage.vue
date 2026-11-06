<template>
  <div class="template-design-page">
    <!-- 顶部工具栏（对齐在线表单设计器：返回 + 标题 + 保存/预览同一行） -->
    <div class="tdp-toolbar">
      <a-button @click="handleBack">
        <ArrowLeftOutlined /> 返回
      </a-button>
      <a-divider type="vertical" />
      <span class="tdp-title">{{ designerRef?.template?.templateName || '模板设计' }}</span>
      <a-tag :color="designerRef?.template?.status === 1 ? 'green' : 'default'" style="margin: 0">
        {{ designerRef?.template?.status === 1 ? '启用' : '停用' }}
      </a-tag>
      <span v-if="designerRef?.template?.templateCode" class="tdp-code">
        {{ designerRef.template.templateCode }}
      </span>

      <div class="tdp-actions">
        <a-button :loading="previewing" @click="handlePreview">
          <PlayCircleOutlined /> 试填充预览
        </a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <SaveOutlined /> 保存设计
        </a-button>
      </div>
    </div>

    <!-- 设计器主体（hideHeader：标题与操作已上移到本页顶栏） -->
    <div class="tdp-body">
      <TemplateDesigner ref="designerRef" :template-id="templateId" hide-header />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Modal } from 'ant-design-vue';
import { ArrowLeftOutlined, PlayCircleOutlined, SaveOutlined } from '@ant-design/icons-vue';

import TemplateDesigner from '../TemplateDesigner.vue';

const route = useRoute();
const router = useRouter();

const templateId = String(route.params.id ?? '');
const designerRef = ref<InstanceType<typeof TemplateDesigner> | null>(null);

const saving = ref(false);
const previewing = ref(false);

/** 保存设计（设计器内部完成落库与提示） */
const handleSave = async () => {
  saving.value = true;
  try {
    await designerRef.value?.save();
  } finally {
    saving.value = false;
  }
};

/** 试填充预览 */
const handlePreview = async () => {
  previewing.value = true;
  try {
    await designerRef.value?.preview();
  } finally {
    previewing.value = false;
  }
};

/** 返回：有未保存改动时确认，避免误丢设计 */
const handleBack = () => {
  if (!designerRef.value?.hasUnsavedChanges()) {
    router.push('/report/template');
    return;
  }
  Modal.confirm({
    title: '有未保存的设计改动',
    content: '返回将丢失未保存的版式/绑定改动，确定返回吗？',
    okText: '放弃并返回',
    cancelText: '留在这里',
    onOk: () => router.push('/report/template'),
  });
};
</script>

<style scoped>
.template-design-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f7;
}

.tdp-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
}

.tdp-title {
  font-weight: 500;
}

.tdp-code {
  font-size: 12px;
  opacity: 0.6;
}

.tdp-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.tdp-body {
  flex: 1;
  min-height: 0;
}
</style>
