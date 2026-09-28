<template>
  <a-drawer v-model:open="open" title="角色权限配置" placement="right" :width="360">
    <a-tree v-if="treeData.length" v-model:checkedKeys="checkedKeys" :tree-data="treeData" :field-names="fieldNames"
      default-expand-all checkable>
    </a-tree>
    <template #footer>
      <a-button type="primary" @click="handleOk" style="float: right;">确定</a-button>
      <a-button @click="handleCancel" style="float: right;margin-right: 10px;">取消</a-button>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { TreeProps } from 'ant-design-vue';
import { getListAllMenu, getListAuthMenu, saveAuthMenu } from '../role.api';
import type { MenuTreeNode } from '../role.types';

/** 查询参数 */
const queryParams = reactive({
  roleId: '',
})
const open = ref(false);

const treeData = ref<MenuTreeNode[]>([]);
const fieldNames: TreeProps['fieldNames'] = {
  children: 'children',
  title: 'title',
  key: 'id'
};
const checkedKeys = ref<string[]>([]);

/** 获取所有菜单、已授权菜单 */
const listMenu = async () => {
  treeData.value = await getListAllMenu();
  checkedKeys.value = await getListAuthMenu(queryParams.roleId);
}

/** 打开弹窗 */
const show = (roleId: string) => {
  open.value = true;
  queryParams.roleId = roleId;
  listMenu();
}

const handleOk = async () => {
  try {
    const res = await saveAuthMenu(queryParams.roleId, checkedKeys.value);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    handleCancel();
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示，此处仅保持抽屉打开
  }
}

const handleCancel = () => {
  open.value = false;
}

//子组件方法默认为私有
defineExpose({
  show
})
</script>