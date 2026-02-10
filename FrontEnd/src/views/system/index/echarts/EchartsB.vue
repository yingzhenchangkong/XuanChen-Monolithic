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
    // 5 条数据（名称 01~05）
    let data = [];
    for (let i = 1; i <= 5; i++) {
      data.push({
        label: String(i).padStart(2, "饼0"),
        arr: Array.from({ length: 20 }).map(() =>
          Math.floor(Math.random() * 500 + 50)
        ),
      });
    }

    // 计算每条的总和，用于饼图
    let pieData = data.map((item, index) => ({
      name: item.label,
      value: item.arr.reduce((a, b) => a + b, 0),
    }));

    // 大屏科技感渐变色
    const gradientColors = [
      ["#4BC5FF", "#0072E6"],
      ["#2DE2C4", "#00A88F"],
      ["#D57EFF", "#7A2CFF"],
      ["#FFAD6B", "#FF6B00"],
      ["#FFF266", "#FFC700"],
    ];

    // 给每个扇区加渐变 + 圆角
    pieData = pieData.map((item, idx) => ({
      ...item,
      itemStyle: {
        borderRadius: 10, // ⭐ 扇形圆角
        color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
          { offset: 0, color: gradientColors[idx][0] },
          { offset: 1, color: gradientColors[idx][1] },
        ]),
        shadowBlur: 18,
        shadowColor: "rgba(0,0,0,0.25)",
      },
    }));

    // ECharts 饼图 option
    const option = {
      backgroundColor: "",
      grid:{
        left: '1%',
        right: '0%',
        top: '5%',
        bottom: '15%',
      },
      tooltip: {
        trigger: "item",
        formatter: "{b} : {c} ({d}%)",
        textStyle: { fontSize: 14 },
      },

      // 图例移到右边（竖排）
      legend: {
        orient: "vertical",
        right: "8%",
        top: "middle",
        textStyle: { color: "#000000", fontSize: 14 },
        itemWidth: 14,
        itemHeight: 14,
      },
      series: [
        {
          name: "数据占比",
          type: "pie",
          radius: ["35%", "65%"],
          center: ["40%", "50%"],  // 为让位图例，整体饼图左移
          label: {
            color: "#000000",
            fontSize: 14,
          },
          labelLine: {
            length: 15,
            length2: 10,
            lineStyle: { color: "#000000" },
          },
          data: pieData,
        },
      ],
    };
    myChart.setOption(option);
  }
});
</script>