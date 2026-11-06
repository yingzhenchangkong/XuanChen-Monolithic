<template>
  <div class="gen-design-page">
    <!-- 顶部工具栏（对齐在线表单设计器：返回 + 标题 + 保存） -->
    <div class="gdp-toolbar">
      <a-button @click="handleBack">
        <ArrowLeftOutlined /> 返回
      </a-button>
      <span class="gdp-title">新增代码生成</span>
      <div class="gdp-spacer"></div>
      <a-button type="primary" :loading="saving" @click="handleSave">
        <SaveOutlined /> 保存并生成
      </a-button>
    </div>

    <!-- 配置主体（与「编辑」弹窗共用 GenTableEditor） -->
    <div class="gdp-body">
      <GenTableEditor ref="editorRef" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons-vue';

import GenTableEditor from '../components/GenTableEditor.vue';

const router = useRouter();

const editorRef = ref<InstanceType<typeof GenTableEditor> | null>(null);
const saving = ref(false);

onMounted(() => {
  editorRef.value?.add();
});

/** 保存并生成：成功后回列表（新增的自动生成提示由组件内部给出） */
const handleSave = async () => {
  saving.value = true;
  try {
    const result = await editorRef.value?.save();
    if (result?.ok) {
      router.push('/generator/generator');
    }
  } finally {
    saving.value = false;
  }
};

const handleBack = () => {
  router.push('/generator/generator');
};
</script>

<style scoped>
.gen-design-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f7;
}

.gdp-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
}

.gdp-title {
  font-weight: 500;
}

.gdp-spacer {
  flex: 1;
}

.gdp-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px 16px;
}
</style>
