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
      backgroundColor: "transparent",
      title: {
        text: "柱1/柱2分析",
        left: "center",
        top: 10,
        textStyle: {
          color: "#000000",
          fontSize: 16,
        },
      },
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "shadow",
        },
        textStyle: {
          color: "#000000",
        },
      },
      legend: {
        data: ["柱1", "柱2"],
        bottom: 10,
        textStyle: {
          color: "#000000",
        },
      },
      grid: {
        left: "3%",
        right: "4%",
        bottom: "15%",
        top: "20%",
        containLabel: true,
      },
      xAxis: [
        {
          type: "category",
          data: ["2010", "2011", "2012", "2013", "2014", "2015", "2016"],
          axisLine: {
            lineStyle: {
              color: "#000000",
            },
          },
          axisLabel: {
            color: "#000000",
          },
        },
      ],
      yAxis: [
        {
          type: "value",
          min: 0,
          max: 100000,
          interval: 20000,
          axisLine: {
            lineStyle: {
              color: "#000000",
            },
          },
          axisLabel: {
            color: "#000000",
            formatter: (value: number) => {
              return value.toLocaleString();
            },
          },
          splitLine: {
            lineStyle: {
              color: "#ffffff",
              opacity: 0.3,
            },
          },
        },
      ],
      series: [
        {
          name: "柱1",
          type: "bar",
          barWidth: "35%",
          barCategoryGap: "20%",
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: {
              type: "linear",
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                {
                  offset: 0,
                  color: "#5FC3E4",
                },
                {
                  offset: 1,
                  color: "#40A8C5",
                },
              ],
            },
          },
          data: [43000, 33000, 68000, 79000, 55000, 62000, 42000],
        },
        {
          name: "柱2",
          type: "bar",
          barWidth: "35%",
          barCategoryGap: "20%",
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: {
              type: "linear",
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                {
                  offset: 0,
                  color: "#4A90E2",
                },
                {
                  offset: 1,
                  color: "#2E5C8A",
                },
              ],
            },
          },
          data: [55000, 63000, 69000, 44000, 53000, 63000, 58000],
        },
      ],
    };
    myChart.setOption(option);
  }
});
</script>