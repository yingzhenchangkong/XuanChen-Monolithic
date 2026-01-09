<template>
  <a-modal v-model:open="visible" :title="modalTitle" :width="400">
    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
      <a-button @click="handleSave">保存</a-button>
    </template>
    <a-tree v-if="treeData.length" v-model:checkedKeys="checkedKeys" :tree-data="treeData" :field-names="fieldNames"
      default-expand-all checkable :height="500">
    </a-tree>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { getAction, postAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';
import type { TreeProps } from 'ant-design-vue';

defineProps({
  modalTitle: {
    type: String,
    default: '分配菜单'
  }
})

const treeData: any = ref([]);
const fieldNames: TreeProps['fieldNames'] = {
  children: 'children',
  title: 'title',
  key: 'id'
};
const checkedKeys = ref<string[]>([]);

const url = {
  listAllMenu: '/system/menu/list',
  listAuthMenu: '/system/rolemenu/listAuthMenu',
  saveAuthMenu: '/system/rolemenu/saveAuthMenu',
}
/** 获取所有菜单、已授权菜单 */
const listMenu = async () => {
  const resAllMenu = await getAction(url.listAllMenu, {});
  treeData.value = resAllMenu.data.records;
  const resAuthMenu = await getAction(url.listAuthMenu, { roleId: queryParams.roleId });
  checkedKeys.value = resAuthMenu.data;
}

/** 查询参数 */
const queryParams = reactive({
  roleId: '',
})
const visible = ref(false);
/** 打开弹窗 */
const show = (roleId: string) => {
  visible.value = true;
  queryParams.roleId = roleId;
  listMenu();
}
/** 关闭弹窗 */
const handleCancel = () => {
  visible.value = false;
}
/** 保存 */
const handleSave = () => {
  postAction(url.saveAuthMenu, { menuIds: checkedKeys.value, roleId: queryParams.roleId }).then((res: any) => {
    message.success(res.msg);
    handleCancel();
  })
}

//子组件方法默认为私有
defineExpose({
  show
})
</script>