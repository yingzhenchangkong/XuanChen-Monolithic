/** 工作台未读通知条目（/dashboard/summary.notices） */
export interface DashboardNotice {
  id: string;
  title: string;
  /** sys_notice_status 行 id，标记已读时使用 */
  noticeStatusId: string;
  /** 发布时间 yyyy-MM-dd HH:mm */
  createTime: string;
}

/** 工作台待办条目（/dashboard/summary.todos） */
export interface DashboardTodo {
  id: string;
  title: string;
  /** 优先级（1高 2中 3低） */
  priority: number;
  /** 截止时间 yyyy-MM-dd HH:mm，可为空 */
  dueDate?: string | null;
  /** 创建时间 yyyy-MM-dd HH:mm */
  createTime: string;
}

/** 工作台汇总（/dashboard/summary） */
export interface DashboardSummary {
  /** 未读公告数 */
  unreadNoticeCount: number;
  /** 未完成待办数 */
  pendingTodoCount: number;
  /** 今日成功登录次数（全系统） */
  todayLoginCount: number;
  /** 未读公告 top5 */
  notices: DashboardNotice[];
  /** 未完成待办 top5 */
  todos: DashboardTodo[];
}

/** 待办（我的待办列表行，结构兼容 DashboardTodo） */
export type TodoItem = DashboardTodo & {
  content?: string | null;
  /** 状态（0待办 1完成） */
  status: number;
  /** 完成时间 yyyy-MM-dd HH:mm:ss，可为空 */
  finishTime?: string | null;
};

/** 新增/编辑待办入参 */
export interface TodoSaveParams {
  id?: string;
  title: string;
  content?: string | null;
  priority: number;
  dueDate?: string | null;
}
