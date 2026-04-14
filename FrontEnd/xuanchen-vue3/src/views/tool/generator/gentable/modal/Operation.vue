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
import { ref, reactive } from 'vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import GenTableConfig from '../modules/GenTableConfig.vue';
import GenTableColumnConfig from '../modules/GenTableColumnConfig.vue';
import GenTableColumnUI from '../modules/GenTableColumnUI.vue';

import { getListTableColumn, getListDBTableColumn } from '../gentable.api';
import { tableColumn } from '../gentable.data';
import type { GenTableColumn } from '../gentable.types';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
});

const emit = defineEmits(['childOK']);
const visible = ref(false);
const isFullScreen = ref(false); // 全屏状态
const dataSource = reactive<GenTableColumn[]>([]);

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

const recGenTableConfig = async (data: any) => {
  await getDataSource(data);
  refGenTableColumnConfig.value?.init(dataSource);
  refGenTableColumnUI.value?.init(dataSource);
}
const recGenTableColumnConfig = async (data: any) => {
  dataSource.splice(0, dataSource.length, ...data);
  refGenTableColumnUI.value?.init(dataSource);
}

const recGenTableColumnUI = async (data: any) => {
  dataSource.splice(0, dataSource.length, ...data);
  refGenTableConfig.value?.init(dataSource);
}

const getDataSource = async (data: any) => {
  const res: any = await getListTableColumn(data.modelDatabase.value.id, data.tableName);
  if (res && res.length != 0) {
    dataSource.splice(0, dataSource.length, ...res);
  } else {
    const res2: any = await getListDBTableColumn(data.modelDatabase.value, data.tableName);
    if (res2 && res2.length != 0) {
      dataSource.splice(0, dataSource.length, ...res2);
    } else {
      const res3: any = tableColumn;
      dataSource.splice(0, dataSource.length, ...res3);
    }
  }
};

defineExpose({
  add,
  edit
})
</script>