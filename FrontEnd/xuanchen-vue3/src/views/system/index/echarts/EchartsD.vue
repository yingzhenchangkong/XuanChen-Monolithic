<template>
  <div ref="chartContainer" style="width: 100%; height: 300px;"></div>
</template>
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts';
const chartContainer = ref<HTMLDivElement | null>(null);
// 实例提升到 setup 作用域：卸载时必须 dispose，否则切页后 canvas、内部 ZRender
// 与事件监听全部残留，反复进出首页会持续泄漏
let myChart: echarts.ECharts | null = null;
// 具名引用 resize 处理器，卸载时才能精确 removeEventListener
const handleResize = () => myChart?.resize();

onMounted(() => {
  if (chartContainer.value) {
    myChart = echarts.init(chartContainer.value);
    window.addEventListener('resize', handleResize);
    const option = {
      radar: {
        indicator: [
          { name: '雷达1', max: 100 },
          { name: '雷达2', max: 100 },
          { name: '雷达3', max: 100 },
          { name: '雷达4', max: 100 },
          { name: '雷达5', max: 100 }
        ],
        axisName: {
          color: '#000000',
          fontSize: 12
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
        data: ['雷达'],
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
          name: '雷达',
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
              name: '雷达'
            }
          ]
        }
      ]
    };
    myChart.setOption(option);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  myChart?.dispose();
  myChart = null;
});
</script>