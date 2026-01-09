import { reactive, ref } from 'vue';
import { getAction, putAction, deleteAction, uploadAction, exportAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';

export const useList = (opts: any) => {
  const {
    url = {},
    queryParams = {}
  } = opts

  /** 数据源 */
  const dataSource = ref([] as any[]);
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
  const operationTitle = ref('')
  /** 打开子组件ID */
  const refOperation = ref()
  /** 定义类型 */
  type Key = string | number;
  /** 已选择行的键 数组 */
  const state = reactive<{ selectedRowKeys: Key[], selectedRows: any[] }>({
    selectedRowKeys: [],
    selectedRows: []
  });
  /** 选择行改变时 改变已选择行的数组 */
  const onSelectChange = (selectedRowKeys: Key[], selectedRows: any[]) => {
    state.selectedRowKeys = selectedRowKeys;
    state.selectedRows = selectedRows;
  };
  /** 取消选择 */
  const handleCancelSelect = () => {
    state.selectedRowKeys = [];
    state.selectedRows = [];
  }
  /** 加载列表 */
  const loadData = () => {
    if (url.list === '') {
      message.error('请设置url.list属性!')
      return
    }
    loading.value = true
    getAction(url.list, queryParams).then((res) => {
      if (res) {
        dataSource.value = res.data.records
        if (res.data.total) {
          ipagination.total = res.data.total
        } else {
          ipagination.total = 0
        }
        handleCancelSelect()
      }
    }).finally(() => {
      loading.value = false
    })
  }
  /** 添加 */
  const handleAdd = () => {
    operationTitle.value = '新增'
    refOperation.value.add()
  }
  /** 编辑 */
  const handleEdit = (record: object) => {
    operationTitle.value = '编辑'
    refOperation.value.edit(record)
  }
  /** 删除 */
  const handleDelete = (id: string) => {
    if (!url.delete) {
      message.error('请设置url.delete属性!')
      return
    }
    deleteAction(url.delete, { id: id }).then((res: any) => {
      if (res.code == 200) {
        message.success(res.msg)
        loadData()
      } else {
        message.warning(res.msg)
      }
    })
  }
  /** 批量删除 */
  const handledeleteBatch = () => {
    if (!url.deleteBatch) {
      message.error('请设置url.deleteBatch属性!')
      return
    }
    if (state.selectedRowKeys.length <= 0) {
      message.warning('请选择一条记录！')
      return
    } else {
      const ids = state.selectedRowKeys.join(',')
      deleteAction(url.deleteBatch, { ids: ids }).then((res: any) => {
        if (res.code == 200) {
          message.success(res.msg)
          loadData()
        } else {
          message.warning(res.msg)
        }
      })
    }
  }
  /** 回收站 还原 */
  const handleRevert = (id: string) => {
    if (!url.revert) {
      message.error('请设置url.revert属性!')
      return
    }
    putAction(url.revert, { id: id }).then((res: any) => {
      if (res.code == 200) {
        message.success(res.msg)
        loadData()
      } else {
        message.warning(res.msg)
      }
    })
  }
  /** 回收站 批量还原*/
  const handleRevertBatch = () => {
    if (!url.revertBatch) {
      message.error('请设置url.revertBatch属性!')
      return
    }
    if (state.selectedRowKeys.length <= 0) {
      message.warning('请选择一条记录！')
      return
    } else {
      const ids = state.selectedRowKeys.join(',')
      putAction(url.revertBatch, { ids: ids }).then((res: any) => {
        if (res.code == 200) {
          message.success(res.msg)
          loadData()
        } else {
          message.warning(res.msg)
        }
      })
    }
  }
  /** 导出 */
  const handleExport = (title: string) => {
    exportAction(url.exportExcel, {}).then((res: any) => {
      const link = document.createElement('a');
      const blob = new Blob([res], { type: 'application/vnd.ms-excel;charset=utf-8' });
      link.style.display = 'none';
      link.href = URL.createObjectURL(blob);
      link.setAttribute('download', title + '.xlsx');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);//下载完成移除元素
      URL.revokeObjectURL(link.href); // 释放URL 对象       
    })
  }
  /** 导入 */
  const handleImport = (info: any) => {
    const formData = new FormData()
    formData.append('file', info.file)
    uploadAction(url.importExcel, formData).then((res: any) => {
      if (res.code == 200) {
        message.success(res.msg)
        loadData()
      } else {
        message.error(res.msg)
      }
    })
  }
  /** 分页改变时触发的函数 */
  const handleTableChange = (pagination: any) => {
    ipagination.current = pagination.current
    ipagination.pageSize = pagination.pageSize
    queryParams.pageNo = pagination.current
    queryParams.pageSize = pagination.pageSize
    loadData()
  }
  /** 导出 */
  return {
    url, queryParams, loadData,
    operationTitle, refOperation,
    handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport, handleRevert, handleRevertBatch,
    dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
  }
}