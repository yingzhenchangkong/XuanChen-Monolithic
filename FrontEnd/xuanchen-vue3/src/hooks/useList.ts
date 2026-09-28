import { reactive, ref } from 'vue';
import { getAction, putAction, deleteAction, uploadAction, exportAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';
import type { Result, PageResult, PageQuery } from '@/types/api';

/** 列表页各操作接口地址集合（均可选，按页面能力传） */
export interface UseListUrlConfig {
  list?: string;
  delete?: string;
  deleteBatch?: string;
  revert?: string;
  revertBatch?: string;
  exportExcel?: string;
  importExcel?: string;
  [key: string]: string | undefined;
}

export interface UseListOptions<T = Record<string, unknown>, Q extends PageQuery = PageQuery> {
  url?: UseListUrlConfig;
  queryParams?: Q;
}

/** 表格行键 */
type RowKey = string | number;

/**
 * 通用列表逻辑：
 * @param T  列表行数据类型（dataSource / selectedRows）
 * @param Q  查询参数类型，需包含分页字段，默认 PageQuery
 */
export const useList = <T = Record<string, unknown>, Q extends PageQuery = PageQuery>(
  opts: UseListOptions<T, Q> = {},
) => {
  const {
    url = {},
    queryParams = {} as Q,
  } = opts

  /** 数据源 */
  const dataSource = ref<T[]>([]);
  /** 分页参数 */
  const ipagination = reactive({
    current: 1,//当前页数
    pageSize: 10,//每页显示XX条数据
    pageSizeOptions: ['10', '20', '50'],//设置pageSize的可选值，页面可以通过下拉框进行选择
    showTotal: (total: number) => `共 ${total} 条`,//展示共有XX条数据
    showQuickJumper: true,//显示跳转到多少页
    showSizeChanger: true,//显示修改pageSize的下拉框
    total: 0,//数据总数
  })
  /** 表格加载状态 */
  const loading = ref(true);
  /** 传入弹窗标题 */
  const operationTitle = ref('');
  /** 打开子组件ID */
  const refOperation = ref<{ add: () => void; edit: (record: T) => void }>();
  /** 已选择行的键与行数据（reactive 对泛型 T 会做深层 Unwrap，此处断言保持 T[] 原样） */
  const state = reactive({
    selectedRowKeys: [] as RowKey[],
    selectedRows: [] as T[],
  }) as { selectedRowKeys: RowKey[], selectedRows: T[] };
  /** 选择行改变时 改变已选择行的数组 */
  const onSelectChange = (selectedRowKeys: RowKey[], selectedRows: T[]) => {
    state.selectedRowKeys = selectedRowKeys;
    state.selectedRows = selectedRows;
  };
  /** 取消选择 */
  const handleCancelSelect = () => {
    state.selectedRowKeys = [];
    state.selectedRows = [];
  }
  /** 加载列表 */
  const loadData = async () => {
    if (!url.list) {
      message.error('请设置url.list属性!');
      return;
    }
    try {
      loading.value = true;
      const res: Result<PageResult<T>> = await getAction<PageResult<T>>(url.list, queryParams);
      // 必须判 code：HTTP 200 但业务失败（如 SQL 异常被统一包装）时 data 可能为 null，
      // 不判 code 直接取 res.data.records 会抛错或静默显示旧/空数据，掩盖真实失败原因。
      // 错误提示由响应拦截器/业务约定统一处理，这里只需保证不用错误响应覆盖表格。
      if (res && res.code === 200 && res.data) {
        dataSource.value = res.data.records;
        ipagination.total = res.data.total || 0;
        handleCancelSelect();
      } else if (res) {
        message.error(res.msg || '列表加载失败！');
      }
    } catch {
      // HTTP 4xx/5xx、超时、断网等已由响应拦截器统一提示，这里不再重复弹错
    } finally {
      loading.value = false;
    }
  }
  /** 添加 */
  const handleAdd = () => {
    operationTitle.value = '新增';
    refOperation.value?.add();
  }
  /** 编辑 */
  const handleEdit = (record: T) => {
    operationTitle.value = '编辑';
    refOperation.value?.edit(record);
  }
  /** 统一处理无数据的增删改响应：成功提示并刷新，失败仅提示后端 msg */
  const isSuccess = (res: Result) => res.code === 200;
  /** 删除 */
  const handleDelete = async (id: RowKey) => {
    if (!url.delete) {
      message.error('请设置url.delete属性!');
      return;
    }
    try {
      const res = await deleteAction(url.delete, { id });
      if (isSuccess(res)) {
        message.success(res.msg);
        await loadData();
      } else {
        message.warning(res.msg);
      }
    } catch {
      message.error('删除失败!');
    }
  }
  /** 批量删除 */
  const handledeleteBatch = async () => {
    if (!url.deleteBatch) {
      message.error('请设置url.deleteBatch属性!');
      return;
    }
    if (state.selectedRowKeys.length <= 0) {
      message.warning('请选择一条记录！');
      return;
    } else {
      const ids = state.selectedRowKeys.join(',');
      try {
        const res = await deleteAction(url.deleteBatch, { ids });
        if (isSuccess(res)) {
          message.success(res.msg);
          await loadData();
        } else {
          message.warning(res.msg);
        }
      } catch {
        message.error('删除失败!');
      }
    }
  }
  /** 回收站 还原 */
  const handleRevert = async (id: RowKey) => {
    if (!url.revert) {
      message.error('请设置url.revert属性!');
      return;
    }
    try {
      const res = await putAction(url.revert, { id });
      if (isSuccess(res)) {
        message.success(res.msg);
        await loadData();
      } else {
        message.warning(res.msg);
      }
    } catch {
      message.error('还原失败!');
    }
  }
  /** 回收站 批量还原*/
  const handleRevertBatch = async () => {
    if (!url.revertBatch) {
      message.error('请设置url.revertBatch属性!');
      return;
    }
    if (state.selectedRowKeys.length <= 0) {
      message.warning('请选择一条记录！');
      return;
    } else {
      const ids = state.selectedRowKeys.join(',');
      try {
        const res = await putAction(url.revertBatch, { ids });
        if (isSuccess(res)) {
          message.success(res.msg);
          await loadData();
        } else {
          message.warning(res.msg);
        }
      } catch {
        message.error('还原失败!');
      }
    }
  }
  /**
   * 解析 Blob 是否为后端 JSON 错误体（未登录/无权限/业务失败时，HTTP 可能仍为 200，
   * 但响应拦截器对 responseType=blob 一律返回 Blob，不解析就会把错误 JSON 当成 Excel 落盘成损坏文件）
   */
  const tryParseJsonBlob = async (blob: Blob): Promise<Result | null> => {
    if (!/json/i.test(blob.type || '')) {
      return null;
    }
    try {
      return JSON.parse(await blob.text()) as Result;
    } catch {
      return null;
    }
  }
  /** 导出 */
  const handleExport = async (title: string) => {
    if (!url.exportExcel) {
      message.error('请设置url.exportExcel属性!');
      return;
    }
    try {
      const res = await exportAction(url.exportExcel, {});
      const data: Blob = res instanceof Blob ? res : new Blob([res]);
      // 保存前拦截：响应是 JSON 错误体时只提示错误，不生成损坏文件
      const errBody = await tryParseJsonBlob(data);
      if (errBody) {
        message.error(errBody.msg || '导出失败!');
        return;
      }
      const link = document.createElement('a');
      // 后端输出的是 .xlsx（OOXML），MIME 必须与之一致，不能再用 xls 的 vnd.ms-excel
      const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      link.style.display = 'none';
      link.href = URL.createObjectURL(blob);
      link.setAttribute('download', title + '.xlsx');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);//下载完成移除元素
      URL.revokeObjectURL(link.href); // 释放URL 对象
    } catch (error) {
      // HTTP 4xx/5xx、超时、断网等已由 httpRequest 响应拦截器统一提示，这里不再重复弹“导出失败”
      console.warn('【useList】导出失败:', error);
    }
  }
  /** 导入（info 为 a-upload change 事件参数中携带原生 File 的片段） */
  const handleImport = async (info: { file: File }) => {
    if (!url.importExcel) {
      message.error('请设置url.importExcel属性!');
      return;
    }
    const formData = new FormData();
    formData.append('file', info.file);
    try {
      const res = await uploadAction(url.importExcel, formData);
      if (isSuccess(res)) {
        message.success(res.msg);
        await loadData();
      } else {
        message.error(res.msg);
      }
    }
    catch {
      message.error('导入失败!');
    }
  }
  /** 分页改变时触发的函数 */
  const handleTableChange = (pagination: { current: number; pageSize: number }) => {
    ipagination.current = pagination.current
    ipagination.pageSize = pagination.pageSize
    queryParams.pageNo = pagination.current
    queryParams.pageSize = pagination.pageSize
    loadData()
  }
  return {
    url, queryParams, loadData,
    operationTitle, refOperation,
    handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport, handleRevert, handleRevertBatch,
    dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
  }
}
