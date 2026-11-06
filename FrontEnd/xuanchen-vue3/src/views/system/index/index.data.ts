/** 待办优先级选项 */
export const TODO_PRIORITY_OPTIONS = [
  { label: '高', value: 1 },
  { label: '中', value: 2 },
  { label: '低', value: 3 },
] as const;

/** 优先级 -> 显示文案 */
export const priorityText = (priority?: number): string =>
  TODO_PRIORITY_OPTIONS.find((item) => item.value === priority)?.label ?? '中';

/** 优先级 -> ant-design-vue 标签色 */
export const priorityColor = (priority?: number): string => {
  switch (priority) {
    case 1:
      return 'red';
    case 3:
      return 'blue';
    default:
      return 'orange';
  }
};
