import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  GridComponent, LegendComponent, TitleComponent, TooltipComponent, GraphicComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { ScreenWidget } from '../visualization.types';

/**
 * 按需注册：只注册大屏用到的图表与组件，避免全量引入拖慢首屏
 * （与 views/system/index/echarts 的处理方式保持一致）
 */
echarts.use([
  BarChart, LineChart, PieChart,
  GridComponent, LegendComponent, TitleComponent, TooltipComponent, GraphicComponent,
  CanvasRenderer,
]);

export { echarts };

const FALLBACK_COLORS = ['#40a9ff', '#36cfc9', '#ffc53d', '#ff7a45', '#9254de', '#73d13d'];

/** 取第一列字符串字段作为类目字段（用户未指定映射时的兜底） */
const guessCategoryField = (rows: Record<string, unknown>[]): string => {
  if (!rows.length) return '';
  const keys = Object.keys(rows[0]);
  const textKey = keys.find((key) => typeof rows[0][key] === 'string');
  return textKey ?? keys[0] ?? '';
};

/** 取第一列数值字段作为指标字段 */
const guessValueField = (rows: Record<string, unknown>[]): string => {
  if (!rows.length) return '';
  const keys = Object.keys(rows[0]);
  const numKey = keys.find((key) => {
    const value = rows[0][key];
    return value != null && value !== '' && !Number.isNaN(Number(value));
  });
  return numKey ?? keys[1] ?? keys[0] ?? '';
};

const toNumber = (value: unknown): number => {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
};

/** 空数据占位（大屏上直接显示「暂无数据」比空白更直观） */
const emptyOption = (widget: ScreenWidget) => ({
  backgroundColor: 'transparent',
  title: {
    text: widget.props?.title || '',
    left: 8,
    top: 4,
    textStyle: { color: '#cfe3ff', fontSize: 14, fontWeight: 'normal' },
  },
  graphic: {
    type: 'text',
    left: 'center',
    top: 'middle',
    style: { text: '暂无数据', fill: 'rgba(207,227,255,0.45)', fontSize: 14 },
  },
});

/**
 * 根据组件配置 + 数据行构建 ECharts option。
 * 只做常用能力（类目轴/数值轴/多系列/饼图），保持大屏图表轻量可控。
 */
export const buildChartOption = (widget: ScreenWidget, rows: Record<string, unknown>[]): any => {
  const props = widget.props ?? {};
  const mapping = widget.data?.mapping ?? {};
  const colors = props.color
    ? (Array.isArray(props.color) ? props.color : [props.color])
    : FALLBACK_COLORS;

  const title = {
    text: props.title || '',
    left: 8,
    top: 4,
    textStyle: { color: '#e6f0ff', fontSize: 14, fontWeight: 'normal' },
  };
  const grid = { left: 16, right: 20, top: props.title ? 44 : 20, bottom: 16, containLabel: true };
  const legend = props.showLegend === false
    ? { show: false }
    : { top: 8, right: 12, textStyle: { color: '#9fc4e8' }, itemWidth: 12, itemHeight: 8 };
  const axisLabel = { color: '#9fc4e8', fontSize: 12 };
  const axisLine = { lineStyle: { color: 'rgba(120,170,220,0.35)' } };
  const splitLine = { lineStyle: { color: 'rgba(120,170,220,0.15)' } };

  if (!rows || rows.length === 0) {
    return emptyOption(widget);
  }

  const categoryField = mapping.x || guessCategoryField(rows);
  const valueField = mapping.y || guessValueField(rows);

  if (widget.type === 'pie') {
    const nameField = mapping.name || categoryField;
    const pieValueField = mapping.value || valueField;
    return {
      backgroundColor: 'transparent',
      color: colors,
      title,
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend,
      series: [
        {
          type: 'pie',
          radius: props.roseType ? ['25%', '65%'] : ['0%', '62%'],
          center: ['50%', '55%'],
          roseType: props.roseType ? 'radius' : undefined,
          avoidLabelOverlap: true,
          label: { color: '#cfe3ff', fontSize: 12 },
          labelLine: { lineStyle: { color: 'rgba(120,170,220,0.45)' } },
          itemStyle: { borderColor: 'rgba(11,16,32,0.6)', borderWidth: 1 },
          data: rows.map((row) => ({
            name: String(row[nameField] ?? ''),
            value: toNumber(row[pieValueField]),
          })),
        },
      ],
    };
  }

  // 柱状图 / 折线图
  const categories = rows.map((row) => String(row[categoryField] ?? ''));
  let series: any[] = [];

  if (mapping.series) {
    // 多系列：按系列字段分组，缺失的组合补 0，避免折线错位
    const groups = new Map<string, Map<string, number>>();
    rows.forEach((row) => {
      const groupKey = String(row[mapping.series as string] ?? '');
      if (!groups.has(groupKey)) groups.set(groupKey, new Map());
      groups.get(groupKey)!.set(String(row[categoryField] ?? ''), toNumber(row[valueField]));
    });
    series = Array.from(groups.entries()).map(([groupKey, valueMap], index) => ({
      name: groupKey,
      type: widget.type === 'bar' ? 'bar' : 'line',
      smooth: widget.type === 'line' ? props.smooth !== false : undefined,
      barMaxWidth: 28,
      itemStyle: { color: colors[index % colors.length] },
      data: categories.map((category) => valueMap.get(category) ?? 0),
    }));
  } else {
    series = [
      {
        name: props.title || '数值',
        type: widget.type === 'bar' ? 'bar' : 'line',
        smooth: widget.type === 'line' ? props.smooth !== false : undefined,
        barMaxWidth: 32,
        areaStyle: widget.type === 'line' && props.area ? { opacity: 0.18 } : undefined,
        itemStyle: { color: colors[0] },
        data: rows.map((row) => toNumber(row[valueField])),
      },
    ];
  }

  return {
    backgroundColor: 'transparent',
    color: colors,
    title,
    tooltip: { trigger: 'axis' },
    legend,
    grid,
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: { ...axisLabel, interval: 0, hideOverlap: true },
      axisLine,
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel,
      axisLine: { show: false },
      splitLine,
    },
    series,
  };
};

/** 数值卡显示值：取首行映射字段，未映射时取首列数值 */
export const resolveNumberValue = (widget: ScreenWidget, rows: Record<string, unknown>[]): number => {
  if (!rows || rows.length === 0) return 0;
  const field = widget.data?.mapping?.y || guessValueField(rows);
  return toNumber(rows[0][field]);
};

/** 表格列：优先用数据集列，其次用首行字段 */
export const resolveTableColumns = (
  rows: Record<string, unknown>[],
  datasetColumns?: string[],
): string[] => {
  if (datasetColumns?.length) return datasetColumns;
  if (rows?.length) return Object.keys(rows[0]);
  return [];
};
