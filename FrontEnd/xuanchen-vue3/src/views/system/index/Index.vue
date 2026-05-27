<template>
  <a-row :gutter="20">
    <a-col :span="6">
      <a-card :bordered="false" style="background-color: #b8f1ef;">
        <template #title>
          <MoneyCollectOutlined style="color: #1677ff;" /> 销售额
        </template>
        <a-statistic :value="yesterdayValue">
          <template #prefix>
            <span style="font-size:xx-small;">昨日</span>
          </template>
        </a-statistic>
        <a-statistic :value="todayValue">
          <template #prefix>
            <span style="font-size:xx-small;">当日</span>
          </template>
          <template #suffix>
            <arrow-up-outlined style="color: green;" v-if="yesterdayValue < todayValue" />
            <arrow-down-outlined style="color: red;" v-else-if="yesterdayValue > todayValue" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>
    <a-col :span="6">
      <a-card :bordered="false" style="background-color: #a8dffe;">
        <template #title>
          <TeamOutlined style="color: #1677ff;" /> 用户数
        </template>
        <a-statistic :value="yesterdayValue">
          <template #prefix>
            <span style="font-size:xx-small;">昨日</span>
          </template>
        </a-statistic>
        <a-statistic :value="todayValue">
          <template #prefix>
            <span style="font-size:xx-small;">当日</span>
          </template>
          <template #suffix>
            <arrow-up-outlined style="color: green;" v-if="yesterdayValue < todayValue" />
            <arrow-down-outlined style="color: red;" v-else-if="yesterdayValue > todayValue" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>
    <a-col :span="6">
      <a-card :bordered="false" style="background-color: #fec8cf;">
        <template #title>
          <AuditOutlined style="color: #1677ff;" /> 订单量
        </template>
        <a-statistic :value="yesterdayValue">
          <template #prefix>
            <span style="font-size:xx-small;">昨日</span>
          </template>
        </a-statistic>
        <a-statistic :value="todayValue">
          <template #prefix>
            <span style="font-size:xx-small;">当日</span>
          </template>
          <template #suffix>
            <arrow-up-outlined style="color: green;" v-if="yesterdayValue < todayValue" />
            <arrow-down-outlined style="color: red;" v-else-if="yesterdayValue > todayValue" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>
    <a-col :span="6">
      <a-card :bordered="false" style="background-color: #a8e9dc;">
        <template #title>
          <GlobalOutlined style="color: #1677ff;" /> 访问量
        </template>
        <a-statistic :value="yesterdayValue">
          <template #prefix>
            <span style="font-size:xx-small;">昨日</span>
          </template>
        </a-statistic>
        <a-statistic :value="todayValue">
          <template #prefix>
            <span style="font-size:xx-small;">当日</span>
          </template>
          <template #suffix>
            <arrow-up-outlined style="color: green;" v-if="yesterdayValue < todayValue" />
            <arrow-down-outlined style="color: red;" v-else-if="yesterdayValue > todayValue" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>
    <a-col :span="24" style="margin-top:10px;margin-bottom: 10px;">
      <a-card title="趋势分析" :bordered="false">
        <echarts-a />
      </a-card>
    </a-col>
    <a-col :span="8">
      <a-card title="饼状图" :bordered="false">
        <echarts-b />
      </a-card>
    </a-col>
    <a-col :span="8">
      <a-card title="柱状图" :bordered="false">
        <echarts-c />
      </a-card>
    </a-col>
    <a-col :span="8">
      <a-card title="雷达图" :bordered="false">
        <echarts-d />
      </a-card>
    </a-col>
  </a-row>

</template>

<script setup lang="ts">
import { ref } from 'vue';

import EchartsA from './echarts/EchartsA.vue';
import EchartsB from './echarts/EchartsB.vue';
import EchartsC from './echarts/EchartsC.vue';
import EchartsD from './echarts/EchartsD.vue';

const yesterdayValue = ref(6666.66);
const todayValue = ref(8888.88);
</script>