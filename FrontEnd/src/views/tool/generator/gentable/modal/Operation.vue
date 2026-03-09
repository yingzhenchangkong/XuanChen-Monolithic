<template>
  <XCModal :open="visible" :title="operationTitle" :defaultFullscreen="true" width="100%" @ok="handleOk"
    @cancel="handleClose">
    <GenTableConfig @childData="recGenTableConfig" ref="refGenTableConfig" />
    <a-tabs defaultActiveKey="1">
      <a-tab-pane key="1" tab="数据库表配置">
        <GenTableColumnConfig @childData="recGenTableColumnConfig" ref="refGenTableColumnConfig" />
      </a-tab-pane>
      <a-tab-pane key="2" tab="页面配置" force-render>
        <GenTableColumnUI @childData="recGenTableColumnUI" ref="refGenTableColumnUI" />
      </a-tab-pane>
    </a-tabs>
  </XCModal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import GenTableConfig from '../modules/GenTableConfig.vue';
import GenTableColumnConfig from '../modules/GenTableColumnConfig.vue';
import GenTableColumnUI from '../modules/GenTableColumnUI.vue';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
});

const emit = defineEmits(['childOK']);
const visible = ref(false);
const isFullScreen = ref(false); // 全屏状态

// 处理关闭
const handleClose = () => {
  visible.value = false;
};
const add = () => {
  visible.value = true;
  isFullScreen.value = true;
}
const edit = (records: any) => {
  visible.value = true;
  isFullScreen.value = true;
};
const handleOk = async () => {
  emit('childOK');
  visible.value = false;
};

const refGenTableConfig = ref();
const refGenTableColumnConfig = ref();
const refGenTableColumnUI = ref();

const recGenTableConfig = (data: any) => {
  refGenTableColumnConfig.value.init(data);
  refGenTableColumnUI.value.init(data);
}
const recGenTableColumnConfig = (data: any) => {
  refGenTableColumnUI.value.init(data);
}

const recGenTableColumnUI = (data: any) => {
  refGenTableConfig.value.init(data);
}

defineExpose({
  add,
  edit
})
</script>