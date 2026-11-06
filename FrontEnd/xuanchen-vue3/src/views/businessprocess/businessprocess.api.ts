import { getAction, postAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type {
  BpmInstanceRecord, BpmTaskRecord, BpmRecordItem,
  BpmSubmitPayload, BpmDiagramState, BpmDelegateRecord, BpmRejectTarget,
  BpmUserOption,
} from './businessprocess.types';

enum BpmApiUrl {
  // ---------- 实例 ----------
  INSTANCE_SUBMIT = '/businessprocess/instance/submit',
  INSTANCE_MINE = '/businessprocess/instance/mine',
  INSTANCE_RECORDS = '/businessprocess/instance/records',
  INSTANCE_DIAGRAM_STATE = '/businessprocess/instance/diagramState',
  INSTANCE_REVOKE = '/businessprocess/instance/revoke',
  INSTANCE_URGE = '/businessprocess/instance/urge',

  // ---------- 任务 ----------
  TASK_TODO = '/businessprocess/task/todo',
  TASK_DONE = '/businessprocess/task/done',
  TASK_APPROVE = '/businessprocess/task/approve',
  TASK_REJECT = '/businessprocess/task/reject',
  TASK_REJECT_TO_NODE = '/businessprocess/task/rejectToNode',
  TASK_REJECT_TARGETS = '/businessprocess/task/rejectTargets',
  TASK_TRANSFER = '/businessprocess/task/transfer',

  // ---------- 委托 ----------
  DELEGATE_LIST = '/businessprocess/common/delegate/list',
  DELEGATE_ADD = '/businessprocess/common/delegate/add',
  DELEGATE_CHANGE_STATUS = '/businessprocess/common/delegate/changeStatus',
  DELEGATE_DELETE = '/businessprocess/common/delegate/delete',

  // ---------- 公共 ----------
  COMMON_USER_SELECT = '/businessprocess/common/userSelect',
}

export { BpmApiUrl };

// ---------- 实例 ----------

export const submitInstanceApi = (data: BpmSubmitPayload): Promise<Result<null>> =>
  postAction(BpmApiUrl.INSTANCE_SUBMIT, data);

export const getMyInstancesApi = (params: Record<string, unknown>): Promise<Result<{ records: BpmInstanceRecord[]; total: number }>> =>
  getAction(BpmApiUrl.INSTANCE_MINE, params);

export const getInstanceRecordsApi = async (instanceId: string): Promise<{ instance: BpmInstanceRecord; records: BpmRecordItem[] }> => {
  const res = await getAction<{ instance: BpmInstanceRecord; records: BpmRecordItem[] }>(
    BpmApiUrl.INSTANCE_RECORDS, { instanceId });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载审批历史失败');
  }
  return res.data;
};

export const getDiagramStateApi = async (instanceId: string): Promise<BpmDiagramState> => {
  const res = await getAction<BpmDiagramState>(BpmApiUrl.INSTANCE_DIAGRAM_STATE, { instanceId });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载流程图状态失败');
  }
  return res.data;
};

export const revokeInstanceApi = (id: string): Promise<Result<null>> =>
  postAction(BpmApiUrl.INSTANCE_REVOKE, { id });

export const urgeInstanceApi = (id: string): Promise<Result<null>> =>
  postAction(BpmApiUrl.INSTANCE_URGE, { id });

// ---------- 任务 ----------

export const getTodoTasksApi = (params: Record<string, unknown>): Promise<Result<{ records: BpmTaskRecord[]; total: number }>> =>
  getAction(BpmApiUrl.TASK_TODO, params);

export const getDoneTasksApi = (params: Record<string, unknown>): Promise<Result<{ records: BpmTaskRecord[]; total: number }>> =>
  getAction(BpmApiUrl.TASK_DONE, params);

export const approveTaskApi = (taskId: string, opinion: string): Promise<Result<null>> =>
  postAction(BpmApiUrl.TASK_APPROVE, { taskId, opinion });

export const rejectTaskApi = (taskId: string, opinion: string): Promise<Result<null>> =>
  postAction(BpmApiUrl.TASK_REJECT, { taskId, opinion });

export const rejectTaskToNodeApi = (taskId: string, targetNodeKey: string, opinion: string): Promise<Result<null>> =>
  postAction(BpmApiUrl.TASK_REJECT_TO_NODE, { taskId, targetNodeKey, opinion });

export const getRejectTargetsApi = async (taskId: string): Promise<BpmRejectTarget[]> => {
  const res = await getAction<BpmRejectTarget[]>(BpmApiUrl.TASK_REJECT_TARGETS, { taskId });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载可驳回节点失败');
  }
  return res.data ?? [];
};

export const transferTaskApi = (taskId: string, targetUser: string, opinion: string): Promise<Result<null>> =>
  postAction(BpmApiUrl.TASK_TRANSFER, { taskId, targetUser, opinion });

// ---------- 委托/代理 ----------

export const getDelegateListApi = (): Promise<Result<BpmDelegateRecord[]>> =>
  getAction(BpmApiUrl.DELEGATE_LIST);

export const addDelegateApi = (data: {
  delegateUserName: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}): Promise<Result<null>> => postAction(BpmApiUrl.DELEGATE_ADD, data);

export const changeDelegateStatusApi = (id: string, status: number): Promise<Result<null>> =>
  postAction(BpmApiUrl.DELEGATE_CHANGE_STATUS, { id, status });

export const deleteDelegateApi = (id: string): Promise<Result<null>> =>
  postAction(BpmApiUrl.DELEGATE_DELETE, { id });

// ---------- 公共 ----------

export const getUserOptionsApi = async (): Promise<BpmUserOption[]> => {
  const res = await getAction<BpmUserOption[]>(BpmApiUrl.COMMON_USER_SELECT);
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载用户列表失败');
  }
  return res.data ?? [];
};
