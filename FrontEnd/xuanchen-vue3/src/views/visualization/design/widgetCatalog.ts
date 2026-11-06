import type { ScreenWidget, WidgetType } from '../visualization.types';

/** 左侧组件面板的分组与默认尺寸/样式 */
export interface WidgetMeta {
  type: WidgetType;
  label: string;
  icon: string;
  group: '文本指标' | '图表' | '其他';
  w: number;
  h: number;
  props: Record<string, any>;
}

export const WIDGET_CATALOG: WidgetMeta[] = [
  {
    type: 'text',
    label: '文本',
    icon: 'FontSizeOutlined',
    group: '文本指标',
    w: 420,
    h: 64,
    props: {
      text: '文本内容',
      fontSize: 28,
      color: '#ffffff',
      fontWeight: 'bold',
      align: 'left',
      background: 'transparent',
      letterSpacing: 0,
    },
  },
  {
    type: 'number',
    label: '数值指标',
    icon: 'DashboardOutlined',
    group: '文本指标',
    w: 320,
    h: 150,
    props: {
      title: '指标名称',
      unit: '',
      precision: 0,
      fontSize: 40,
      color: '#40a9ff',
      titleColor: '#cfe3ff',
      background: 'rgba(23,44,86,0.55)',
      borderColor: 'rgba(64,169,255,0.35)',
    },
  },
  {
    type: 'bar',
    label: '柱状图',
    icon: 'BarChartOutlined',
    group: '图表',
    w: 620,
    h: 340,
    props: { title: '柱状图', color: '', showLegend: true },
  },
  {
    type: 'line',
    label: '折线图',
    icon: 'LineChartOutlined',
    group: '图表',
    w: 620,
    h: 340,
    props: { title: '折线图', color: '', showLegend: true, smooth: true, area: true },
  },
  {
    type: 'pie',
    label: '饼图',
    icon: 'PieChartOutlined',
    group: '图表',
    w: 460,
    h: 340,
    props: { title: '饼图', color: '', showLegend: true, roseType: false },
  },
  {
    type: 'table',
    label: '表格',
    icon: 'TableOutlined',
    group: '图表',
    w: 720,
    h: 360,
    props: {
      title: '数据表格',
      headerBg: 'rgba(64,169,255,0.18)',
      fontSize: 14,
      color: '#e6f0ff',
      striped: true,
      maxRows: 10,
    },
  },
  {
    type: 'image',
    label: '图片',
    icon: 'PictureOutlined',
    group: '其他',
    w: 400,
    h: 260,
    props: { src: '', fit: 'cover', radius: 4 },
  },
  {
    type: 'border',
    label: '边框容器',
    icon: 'BorderOutlined',
    group: '其他',
    w: 700,
    h: 400,
    props: { title: '分组标题', borderColor: 'rgba(64,169,255,0.45)', color: '#40a9ff', background: 'rgba(13,32,64,0.35)' },
  },
];

export const getWidgetMeta = (type: WidgetType): WidgetMeta =>
  WIDGET_CATALOG.find((item) => item.type === type) ?? WIDGET_CATALOG[0];

/** 生成组件实例：拖入画布时调用，尺寸按目录默认值，位置由画布决定 */
export const createWidget = (type: WidgetType, x: number, y: number, zIndex: number): ScreenWidget => {
  const meta = getWidgetMeta(type);
  return {
    id: `w_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`,
    type,
    name: meta.label,
    x: Math.round(x),
    y: Math.round(y),
    w: meta.w,
    h: meta.h,
    zIndex,
    props: JSON.parse(JSON.stringify(meta.props)),
    data: { mode: 'static', datasetKey: '', staticRows: [], mapping: {} },
  };
};

/** 组件是否需要数据集（文本/图片/边框不绑定数据） */
export const isDataWidget = (type: WidgetType): boolean =>
  ['number', 'bar', 'line', 'pie', 'table'].includes(type);
