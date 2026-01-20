<template>
  <a-row :gutter="10">
    <a-col :span="10">
      <DeptTree @childData="recDataDeptTree" ref="refDeptTree" />
    </a-col>
    <a-col :span="14">
      <DeptInfo @childData="recDataDeptInfo" ref="refDeptInfo" />
    </a-col>
  </a-row>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import DeptTree from './modules/DeptTree.vue';
import DeptInfo from './modules/DeptInfo.vue';
import type { DeptTranData } from './dept.types';

const refDeptTree = ref();
const recDataDeptTree = (data: DeptTranData) => {
  refDeptInfo.value.recParentData(data);
};

const refDeptInfo = ref();
const recDataDeptInfo = (data: DeptTranData) => {
  refDeptTree.value.recParentData(data);
};
</script>