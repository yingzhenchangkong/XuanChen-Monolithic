<template>
  <a-modal v-model:open="visible" :title="modalTitle" :width="1000">
    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
    </template>
    <a-row :gutter="20">
      <a-col :span="12">
        <a-table :dataSource="dataSourceUnAssignUsers" :columns="columnsAssignUser" :pagination="ipaginationUnAssignUsers"
          :loading="loadingUnAssignUsers"
          :row-selection="{ selectedRowKeys: stateUnAssignUsers.selectedRowKeysUnAssignUsers, onChange: onSelectChangeUnAssignUsers }"
          bordered rowKey="id" size="small" @change="handleTableChangeUnAssignUsers">
          <template #title>未分配员工
            <template v-if="stateUnAssignUsers.selectedRowKeysUnAssignUsers.length > 0">
              <span class="btn-style">
                <a-button type="primary" size="small" @click="handleAssignBatch">批量分配</a-button>
              </span>
              <a-tag color="processing">
                {{ `已选中 ${stateUnAssignUsers.selectedRowKeysUnAssignUsers.length} 条记录` }}
                <a @click="handleCancelSelectUnAssignUser">清空</a>
              </a-tag>
            </template>
          </template>
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'operation'">
              <a @click="handleAssign(record)">
                <UserAddOutlined />分配
              </a>
            </template>
          </template>
        </a-table>
      </a-col>
      <a-col :span="12">
        <a-table :dataSource="dataSource" :columns="columnsAssignUser" :pagination="ipagination" :loading="loading"
          :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
          size="small" @change="handleTableChange">
          <template #title>已分配员工
            <template v-if="state.selectedRowKeys.length > 0">
              <span class="btn-style">
                <a-button type="primary" size="small" @click="handleCancleAssignBatch">批量取消分配</a-button>
              </span>
              <a-tag color="processing">
                {{ `已选中 ${state.selectedRowKeys.length} 条记录` }}
                <a @click="handleCancelSelect">清空</a>
              </a-tag>
            </template>
          </template>
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'operation'">
              <a @click="handleCancleAssign(record)">
                <UserDeleteOutlined />取消分配
              </a>
            </template>
          </template>
        </a-table>
      </a-col>
    </a-row>
  </a-modal>
</template>

<script setup lang="ts">
import { useList } from '@/hooks/useList'
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { columnsAssignUser } from '../post.data';
import { PostApiUrl, cancelAssignUser, cancleAssignUserBatch, assignUser, assignUserBatch, getListUnAssignUser } from '../post.api';

defineProps({
  modalTitle: {
    type: String,
    default: '分配员工'
  }
})

const url = {
  list: PostApiUrl.ASSIGN_USER_LIST_ASSIGN_USER,
}
/** 查询参数 */
const queryParams = reactive({
  postId: '',
})
const visible = ref(false);
const show = (postId: string) => {
  visible.value = true;
  handleCancelSelect();
  handleCancelSelectUnAssignUser();
  queryParams.postId = postId;
  refresh()
}

const refresh = () => {
  loadData()
  loadDataUnAssignUser()
}

const handleCancel = () => {
  visible.value = false;
}
/** 取消授权 */
const handleCancleAssign = async (record: any) => {
  const res: any = await cancelAssignUser(record.userId, queryParams.postId);
  message.success(res.msg);
  refresh();
}
/** 批量取消授权 */
const handleCancleAssignBatch = async () => {
  const userIds = state.selectedRowKeys.map(key => String(key));
  const res: any = await cancleAssignUserBatch(userIds, queryParams.postId);
  message.success(res.msg);
  refresh();
}
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })


const loadingUnAssignUsers = ref(true);
const dataSourceUnAssignUsers = ref([]);
const queryParamsUnAssignUsers = reactive({
  postId: '',
  pageNo: 1,
  pageSize: 5
})
const ipaginationUnAssignUsers = reactive({
  current: 1,//当前页数
  pageSize: 5,//每页显示XX条数据
  pageSizeOptions: ['10', '20', '50'],//设置pageSize的可选值，页面可以通过下拉框进行选择
  showTotal: (total: number) => `共 ${total} 条`,//展示共有XX条数据
  showQuickJumper: true,//显示跳转到多少页
  showSizeChanger: true,//显示修改pageSize的下拉框
  total: 0,//数据总数
})
type KeyUnAssignUsers = string | number;
/** 已选择行的键 数组 */
const stateUnAssignUsers = reactive<{ selectedRowKeysUnAssignUsers: KeyUnAssignUsers[] }>({
  selectedRowKeysUnAssignUsers: [],
});
/** 选择行改变时 改变已选择行的数组 */
const onSelectChangeUnAssignUsers = (selectedRowKeysUnAssignUsers: KeyUnAssignUsers[]) => {
  stateUnAssignUsers.selectedRowKeysUnAssignUsers = selectedRowKeysUnAssignUsers;
};
const handleTableChangeUnAssignUsers = (paginationUnAssignUsers: any) => {
  ipaginationUnAssignUsers.current = paginationUnAssignUsers.current
  ipaginationUnAssignUsers.pageSize = paginationUnAssignUsers.pageSize
  queryParamsUnAssignUsers.pageNo = paginationUnAssignUsers.current
  queryParamsUnAssignUsers.pageSize = paginationUnAssignUsers.pageSize
  loadData()
}
/** 取消选择 */
const handleCancelSelectUnAssignUser = () => {
  stateUnAssignUsers.selectedRowKeysUnAssignUsers = []
}
/** 授权 */
const handleAssign = async (record: any) => {
  const res: any = await assignUser(record.userId, queryParams.postId);
  message.success(res.msg);
  refresh();
}
/** 批量授权 */
const handleAssignBatch = async () => {
  const userIds = stateUnAssignUsers.selectedRowKeysUnAssignUsers.map(key => String(key));
  const res: any = await assignUserBatch(userIds, queryParams.postId);
  message.success(res.msg);
  refresh();
}
const loadDataUnAssignUser = async () => {
  loadingUnAssignUsers.value = true;
  const res: any = await getListUnAssignUser(queryParams.postId);
  dataSourceUnAssignUsers.value = res.data.records;
  if (res.data.total) {
    ipaginationUnAssignUsers.total = res.data.total
  } else {
    ipaginationUnAssignUsers.total = 0
  }
  handleCancelSelect();
  loadingUnAssignUsers.value = false;
}

//子组件方法默认为私有
defineExpose({
  show
})
</script>