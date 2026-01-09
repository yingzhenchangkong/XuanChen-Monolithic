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
      radar: {
        indicator: [
          { name: '1', max: 100 },
          { name: '2', max: 100 },
          { name: '3', max: 100 },
          { name: '4', max: 100 },
          { name: '5', max: 100 }
        ],
        name: {
          textStyle: {
            color: '#000000',
            fontSize: 12
          }
        },
        splitLine: {
          lineStyle: {
            color: 'rgba(144, 224, 239, 0.5)'
          }
        },
        splitArea: {
          show: false
        },
        axisLine: {
          lineStyle: {
            color: 'rgba(144, 224, 239, 0.8)'
          }
        },

        center: ['50%', '45%'],
        radius: '70%'
      },

      legend: {
        data: ['text'],
        textStyle: {
          color: '#000000',
          fontSize: 12
        },
        bottom: '2%',
        left: 'center',
        icon: 'circle',
        itemWidth: 8,
        itemGap: 5
      },
      series: [
        {
          name: 'text',
          type: 'radar',
          areaStyle: {
            color: 'rgba(144, 224, 239, 0.3)'
          },
          lineStyle: {
            color: 'rgba(144, 224, 239, 0.8)',
            width: 2
          },
          data: [
            {
              value: [60, 40, 50, 80, 70],
              name: 'text'
            }
          ]
        }
      ]
    };
    myChart.setOption(option);
  }
});
</script>