<template>
  <a-row :gutter="10">
    <a-col :span="8">
      <a-card title="服务器">
        <a-list item-layout="horizontal">
          <a-list-item>
            Redis版本：<span style="color: green;">{{ cache.version }}</span>
          </a-list-item>
          <a-list-item>
            操作系统：<span style="color: green;">{{ cache.os }}{{ cache.archBits }}</span>
          </a-list-item>
          <a-list-item>
            进程ID：<span style="color: green;">{{ cache.processId }}</span>
          </a-list-item>
        </a-list>
      </a-card>
    </a-col>
    <a-col :span="8">
      <a-card title="内存">
        <a-list item-layout="horizontal">
          <a-list-item>
            已用内存：<span style="color: green;">{{ cache.usedMemoryHuman }}</span>
          </a-list-item>
          <a-list-item>
            内存占用峰值：<span style="color: green;">{{ cache.usedMemoryPeakHuman }}</span>
          </a-list-item>
          <a-list-item>
            Lua占用内存：<span style="color: green;">{{ cache.usedMemoryLuaHuman }}</span>
          </a-list-item>
        </a-list>
      </a-card>
    </a-col>
    <a-col :span="8">
      <a-card title="状态">
        <a-list item-layout="horizontal">
          <a-list-item>
            客户端连接数：<span style="color: green;">{{ cache.connectedClients }}</span>
          </a-list-item>
          <a-list-item>
            历史连接数：<span style="color: green;">{{ cache.totalConnectionsReceived }}</span>
          </a-list-item>
          <a-list-item>
            历史命令数：<span style="color: green;">{{ cache.totalCommandsProcessed }}</span>
          </a-list-item>
        </a-list>
      </a-card>
    </a-col>
  </a-row>
  <a-row>
    <a-col :span="24">
      <a-card title="Redis信息全集">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false"></a-table>
      </a-card>
    </a-col>
  </a-row>

</template>

<script setup lang="ts">
import { ref } from 'vue';

import { getCache } from './cache.api';
import { columns } from './cache.data';

const cache = ref<any>([]);
const dataSource = ref<any>([]);
const getRedisInfo = async () => {
  const res = await getCache();
  cache.value = res.data;
  dataSource.value = dataSource.value.concat({ description:"运行模式",key: "redis_mode", value: res.data.mode });
  dataSource.value = dataSource.value.concat({ description:"最大内存配置值",key: "max_memory", value: res.data.maxMemory });
  dataSource.value = dataSource.value.concat({ description:"最大客户端连接数",key: "max_clients", value: res.data.maxClients });
  dataSource.value = dataSource.value.concat({ description:"数据库大小",key: "db_size", value: res.data.dbSize });
};
getRedisInfo();
</script>