<template>
  <div ref="chartContainer" style="width: 100%; height: 300px;"></div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import * as echarts from 'echarts';
const chartContainer = ref<HTMLDivElement | null>(null);

onMounted(() => {
  if (chartContainer.value) {
    const myChart = echarts.init(chartContainer.value);
    const option = {
      grid: {
        left: '1%',
        right: '0%',
        top: '5%',
        bottom: '5%',
      },
      xAxis: {
        type: 'category',
        data: ['00:00', '01:00', '02:00', '03:00', '04:00', '05:00', '06:00', '07:00', '08:00','09:00','10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'],
        axisLabel: { color: '#000000' },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 80,
        interval: 20,
        axisLabel: { color: '#000000' },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: {
          lineStyle: { color: 'rgba(255, 255, 255, 0.2)' }
        }
      },
      series: [
        {
          name: '趋势分析',
          type: 'line',
          smooth: true,

          lineStyle: { color: '#409EFF', width: 2 },
          itemStyle: { color: '#409EFF' },

          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.8)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0)' }
            ])
          },
          data: [35, 55, 45, 35, 50, 60, 70, 65, 55, 45, 35, 50, 60, 70, 65, 55, 45, 35, 50, 60, 70, 65, 55, 45]
        }
      ],
      legend: {
        data: ['趋势分析'],
        textStyle: { color: '#000000' },
        bottom: '90%',
        left: 'center',
        icon: 'circle'
      }
    };
    myChart.setOption(option);
  }
});
</script>