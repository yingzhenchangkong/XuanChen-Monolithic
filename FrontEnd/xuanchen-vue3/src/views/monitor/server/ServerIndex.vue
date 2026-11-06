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
import type { ServerInfo } from './server.types';
import { columns } from './server.data';

/** 监控信息表格行 */
interface ServerInfoRow {
  description: string;
  value: unknown;
}

const info = ref<ServerInfo>();
const dataSourceCPU = ref<ServerInfoRow[]>([]);
const dataSourceMemory = ref<ServerInfoRow[]>([]);
const dataSourceDisk = ref<ServerInfoRow[]>([]);
const dataSourceJVM = ref<ServerInfoRow[]>([]);
const getRedisInfo = async () => {
  const res = await getServerInfoApi();
  info.value = res.data;
  // CPU信息
  dataSourceCPU.value = [
    { description: "CPU核心数", value: res.data.cpuNum },
    { description: "CPU使用率", value: res.data.cpuUsed },
    { description: "CPU系统使用率", value: res.data.cpuSys },
    { description: "CPU剩余率", value: res.data.cpuFree },
  ];
  // 内存信息
  dataSourceMemory.value = [
    { description: "总内存(G)", value: res.data.totalMem },
    { description: "已用内存(G)", value: res.data.usedMem },
    { description: "剩余内存(G)", value: res.data.freeMem },
    { description: "内存使用率", value: res.data.memUsage },
  ];
  // 磁盘信息
  dataSourceDisk.value = res.data.diskInfos?.flatMap((item) => [
    { description: "磁盘名称", value: item.name },
    { description: "磁盘总容量(G)", value: item.total },
    { description: "磁盘剩余容量(G)", value: item.free },
    { description: "磁盘已用容量(G)", value: item.used },
    { description: "磁盘使用率", value: item.usage },
  ]) ?? [];
  // JVM信息
  dataSourceJVM.value = [
    { description: "JVM名称", value: res.data.jvmName },
    { description: "JVM版本", value: res.data.jvmVersion },
    { description: "JVM总内存(M)", value: res.data.jvmTotal },
    { description: "JVM最大内存(M)", value: res.data.jvmMax },
    { description: "JVM空闲内存(M)", value: res.data.jvmFree },
    { description: "JVM已用内存(M)", value: res.data.jvmUsed },
    { description: "JVM使用率", value: res.data.jvmUsage },
  ];
};
getRedisInfo();
</script>