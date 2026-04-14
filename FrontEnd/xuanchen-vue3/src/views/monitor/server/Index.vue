<template>
  <a-tabs defaultActiveKey="1">
    <a-tab-pane key="1" tab="CPU信息">
      <a-table :columns="columns" :data-source="dataSourceCPU" :pagination="false"></a-table>
    </a-tab-pane>
    <a-tab-pane key="2" tab="内存信息">
      <a-table :columns="columns" :data-source="dataSourceMemory" :pagination="false"></a-table>
    </a-tab-pane>
    <a-tab-pane key="3" tab="磁盘信息">
      <a-table :columns="columns" :data-source="dataSourceDisk" :pagination="false"></a-table>
    </a-tab-pane>
    <a-tab-pane key="4" tab="JVM信息">
      <a-table :columns="columns" :data-source="dataSourceJVM" :pagination="false"></a-table>
    </a-tab-pane>
  </a-tabs>
</template>
<script setup lang="ts">
import { ref } from 'vue';

import { getServerInfoApi } from './server.api';
import { columns } from './server.data';

const info = ref<any>([]);
const dataSourceCPU = ref<any>([]);
const dataSourceMemory = ref<any>([]);
const dataSourceDisk = ref<any>([]);
const dataSourceJVM = ref<any>([]);
const getRedisInfo = async () => {
  const res = await getServerInfoApi();
  info.value = res.data;
  // CPU信息
  dataSourceCPU.value = dataSourceCPU.value.concat({ description: "CPU核心数", value: res.data.cpuNum });
  dataSourceCPU.value = dataSourceCPU.value.concat({ description: "CPU使用率", value: res.data.cpuUsed });
  dataSourceCPU.value = dataSourceCPU.value.concat({ description: "CPU系统使用率", value: res.data.cpuSys });
  dataSourceCPU.value = dataSourceCPU.value.concat({ description: "CPU剩余率", value: res.data.cpuFree });
  // 内存信息
  dataSourceMemory.value = dataSourceMemory.value.concat({ description: "总内存(G)", value: res.data.totalMem });
  dataSourceMemory.value = dataSourceMemory.value.concat({ description: "已用内存(G)", value: res.data.usedMem });
  dataSourceMemory.value = dataSourceMemory.value.concat({ description: "剩余内存(G)", value: res.data.freeMem });
  dataSourceMemory.value = dataSourceMemory.value.concat({ description: "内存使用率", value: res.data.memUsage });
  // 磁盘信息
  res.data.diskInfos.forEach((item: any) => {
    dataSourceDisk.value = dataSourceDisk.value.concat({ description: "磁盘名称", value: item.name });
    dataSourceDisk.value = dataSourceDisk.value.concat({ description: "磁盘总容量(G)", value: item.total });
    dataSourceDisk.value = dataSourceDisk.value.concat({ description: "磁盘剩余容量(G)", value: item.free });
    dataSourceDisk.value = dataSourceDisk.value.concat({ description: "磁盘已用容量(G)", value: item.used });
    dataSourceDisk.value = dataSourceDisk.value.concat({ description: "磁盘使用率", value: item.usage });
  });
  // JVM信息
  dataSourceJVM.value = dataSourceJVM.value.concat({ description: "JVM名称", value: res.data.jvmName });
  dataSourceJVM.value = dataSourceJVM.value.concat({ description: "JVM版本", value: res.data.jvmVersion });
  dataSourceJVM.value = dataSourceJVM.value.concat({ description: "JVM总内存(M)", value: res.data.jvmTotal });
  dataSourceJVM.value = dataSourceJVM.value.concat({ description: "JVM最大内存(M)", value: res.data.jvmMax });
  dataSourceJVM.value = dataSourceJVM.value.concat({ description: "JVM空闲内存(M)", value: res.data.jvmFree });
  dataSourceJVM.value = dataSourceJVM.value.concat({ description: "JJVM已用内存(M)", value: res.data.jvmUsed });
  dataSourceJVM.value = dataSourceJVM.value.concat({ description: "JVM使用率", value: res.data.jvmUsage });
};
getRedisInfo();
</script>