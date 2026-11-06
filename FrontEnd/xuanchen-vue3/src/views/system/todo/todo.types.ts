/** 待办记录 */
export interface TodoRecord {
  id: string;
  title: string;
  priority?: number;
  status: number;
  description?: string;
  createTime?: string;
  finishTime?: string;
  bpmTaskId?: string;
}

/** 新增/编辑提交参数 */
export interface TodoSaveParams {
  id?: string;
  title: string;
  priority?: number;
  description?: string;
}
