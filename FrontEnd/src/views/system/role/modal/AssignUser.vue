<template>
  <a-modal v-model:open="visible" :title="modalTitle" :width="1000">
    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
    </template>
    <a-row :gutter="20">
      <a-col :span="12">
        <a-table :dataSource="dataSourceUnAuthUsers" :columns="columnsAssignUser" :pagination="ipaginationUnAuthUsers"
          :loading="loadingUnAuthUsers"
          :row-selection="{ selectedRowKeys: stateUnAuthUsers.selectedRowKeysUnAuthUsers, onChange: onSelectChangeUnAuthUsers }"
          bordered rowKey="id" size="small" @change="handleTableChangeUnAuthUsers">
          <template #title>未授权用户
            <template v-if="stateUnAuthUsers.selectedRowKeysUnAuthUsers.length > 0">
              <span class="btn-style">
                <a-button type="primary" size="small" @click="handleAuthBatch">批量授权</a-button>
              </span>
              <a-tag color="processing">
                {{ `已选中 ${stateUnAuthUsers.selectedRowKeysUnAuthUsers.length} 条记录` }}
                <a @click="handleCancelSelectUnAuthUser">清空</a>
              </a-tag>
            </template>
          </template>
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'operation'">
              <a @click="handleAuth(record)">
                <UserAddOutlined />授权
              </a>
            </template>
          </template>
        </a-table>
      </a-col>
      <a-col :span="12">
        <a-table :dataSource="dataSource" :columns="columnsAssignUser" :pagination="ipagination" :loading="loading"
          :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
          size="small" @change="handleTableChange">
          <template #title>已授权用户
            <template v-if="state.selectedRowKeys.length > 0">
              <span class="btn-style">
                <a-button type="primary" size="small" @click="handleCancleAuthBatch">批量取消授权</a-button>
              </span>
              <a-tag color="processing">
                {{ `已选中 ${state.selectedRowKeys.length} 条记录` }}
                <a @click="handleCancelSelect">清空</a>
              </a-tag>
            </template>
          </template>
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'operation'">
              <a @click="handleCancleAuth(record)">
                <UserDeleteOutlined />取消授权
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
import { columnsAssignUser } from '../role.data';
import { RoleApiUrl, cancelAuthUser, cancleAuthUserBatch, authUser, authUserBatch, getListUnAuthUser } from '../role.api';

defineProps({
  modalTitle: {
    type: String,
    default: '分配用户'
  }
})

const url = {
  list: RoleApiUrl.ASSIGN_USER_LIST_AUTH_USER,
}
/** 查询参数 */
const queryParams = reactive({
  roleId: '',
})
const visible = ref(false);
const show = (roleId: string) => {
  visible.value = true;
  handleCancelSelect();
  handleCancelSelectUnAuthUser();
  queryParams.roleId = roleId;
  refresh()
}

const refresh = () => {
  loadData()
  loadDataUnAuthUser()
}

const handleCancel = () => {
  visible.value = false;
}
/** 取消授权 */
const handleCancleAuth = async (record: any) => {
  const res: any = await cancelAuthUser(record.userId, queryParams.roleId);
  message.success(res.msg);
  refresh();
}
/** 批量取消授权 */
const handleCancleAuthBatch = async () => {
  const userIds = state.selectedRowKeys.map(key => String(key));
  const res: any = await cancleAuthUserBatch(userIds, queryParams.roleId);
  message.success(res.msg);
  refresh();
}
const {
  loadData,
  dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
} = useList({ url, queryParams })


const loadingUnAuthUsers = ref(true);
const dataSourceUnAuthUsers = ref([]);
const queryParamsUnAuthUsers = reactive({
  roleId: '',
  pageNo: 1,
  pageSize: 5
})
const ipaginationUnAuthUsers = reactive({
  current: 1,//当前页数
  pageSize: 5,//每页显示XX条数据
  pageSizeOptions: ['10', '20', '50'],//设置pageSize的可选值，页面可以通过下拉框进行选择
  showTotal: (total: number) => `共 ${total} 条`,//展示共有XX条数据
  showQuickJumper: true,//显示跳转到多少页
  showSizeChanger: true,//显示修改pageSize的下拉框
  total: 0,//数据总数
})
type KeyUnAuthUsers = string | number;
/** 已选择行的键 数组 */
const stateUnAuthUsers = reactive<{ selectedRowKeysUnAuthUsers: KeyUnAuthUsers[] }>({
  selectedRowKeysUnAuthUsers: [],
});
/** 选择行改变时 改变已选择行的数组 */
const onSelectChangeUnAuthUsers = (selectedRowKeysUnAuthUsers: KeyUnAuthUsers[]) => {
  stateUnAuthUsers.selectedRowKeysUnAuthUsers = selectedRowKeysUnAuthUsers;
};
const handleTableChangeUnAuthUsers = (paginationUnAuthUsers: any) => {
  ipaginationUnAuthUsers.current = paginationUnAuthUsers.current
  ipaginationUnAuthUsers.pageSize = paginationUnAuthUsers.pageSize
  queryParamsUnAuthUsers.pageNo = paginationUnAuthUsers.current
  queryParamsUnAuthUsers.pageSize = paginationUnAuthUsers.pageSize
  loadData()
}
/** 取消选择 */
const handleCancelSelectUnAuthUser = () => {
  stateUnAuthUsers.selectedRowKeysUnAuthUsers = []
}
/** 授权 */
const handleAuth = async (record: any) => {
  const res: any = await authUser(record.userId, queryParams.roleId);
  message.success(res.msg);
  refresh();
}
/** 批量授权 */
const handleAuthBatch = async () => {
  const userIds = stateUnAuthUsers.selectedRowKeysUnAuthUsers.map(key => String(key));
  const res: any = await authUserBatch(userIds, queryParams.roleId);
  message.success(res.msg);
  refresh();
}
const loadDataUnAuthUser = async () => {
  loadingUnAuthUsers.value = true;
  const res: any = await getListUnAuthUser(queryParams.roleId);
  dataSourceUnAuthUsers.value = res.data.records;
  if (res.data.total) {
    ipaginationUnAuthUsers.total = res.data.total
  } else {
    ipaginationUnAuthUsers.total = 0
  }
  handleCancelSelect();
  loadingUnAuthUsers.value = false;
}

//子组件方法默认为私有
defineExpose({
  show
})
</script>