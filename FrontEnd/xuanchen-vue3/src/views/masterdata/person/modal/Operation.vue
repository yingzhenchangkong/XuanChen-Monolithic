<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="1120" :show-fullscreen-btn="false" @ok="handleOk">
    <div class="person-modal-body">
      <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autocomplete="off" class="modal-form-style">
        <!-- 部门 / 岗位：关系表 sys_person_dept / sys_person_post，随人员一并保存，置于表单最前；
             照片为 sys_person.photo（存上传后的相对路径）。
             照片上传区是 antd picture-card（104px 高），约等于两行表单项的高度，
             故左侧排两行（部门/岗位 + 姓名/性别）、照片占右侧 1/3 跨两行，避免首行下方留白 -->
        <a-row style="width: 100%">
          <a-col :span="16">
            <a-row style="width: 100%">
              <a-col :span="12">
                <a-form-item name="deptIds" label="部门" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-tree-select v-model:value="model.deptIds" show-search multiple allow-clear tree-default-expand-all
                    :dropdown-style="{ maxHeight: '360px', overflow: 'auto' }" style="width: 100%" :tree-data="deptTree"
                    tree-node-filter-prop="title" placeholder="请选择部门"
                    :field-names="{ children: 'children', label: 'title', value: 'key' }" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item name="postIds" label="岗位" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select v-model:value="model.postIds" mode="multiple" allow-clear style="width: 100%"
                    :options="postOptions" :fieldNames="{ label: 'postName', value: 'id' }" placeholder="请选择岗位"
                    :max-tag-count="1">
                    <template #maxTagPlaceholder="omittedValues">
                      <span style="color: red">+ {{ omittedValues.length }} ...</span>
                    </template>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row style="width: 100%">
              <a-col :span="12">
                <a-form-item name="realName" label="姓名" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input v-model:value="model.realName" placeholder="请输入姓名" allowClear />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item name="gender" label="性别" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select v-model:value="model.gender" style="width: 100%" :options="genderOptions"
                    placeholder="请选择性别" allowClear />
                </a-form-item>
              </a-col>
            </a-row>
          </a-col>
          <a-col :span="8">
            <a-form-item name="photo" label="照片" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <XCUploadImage v-model:file-list="photoFiles" image-path="photo" :max-count="1" :max-size="2" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%">
          <a-col :span="8">
            <a-form-item name="birthday" label="出生日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-date-picker v-model:value="model.birthday" value-format="YYYY-MM-DD" style="width: 100%"
                placeholder="请选择出生日期" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="idCard" label="身份证号" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.idCard" placeholder="请输入身份证号" allowClear :maxlength="18" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="nation" label="民族" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.nation" placeholder="请输入民族" allowClear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%">
          <a-col :span="8">
            <a-form-item name="nativePlace" label="籍贯" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.nativePlace" placeholder="请输入籍贯" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="maritalStatus" label="婚姻状况" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-select v-model:value="model.maritalStatus" style="width: 100%" :options="maritalOptions"
                placeholder="请选择婚姻状况" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="education" label="学历" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-select v-model:value="model.education" style="width: 100%" :options="educationOptions"
                placeholder="请选择学历" allowClear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%">
          <a-col :span="8">
            <a-form-item name="mobile" label="手机号" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.mobile" placeholder="请输入手机号" allowClear :maxlength="11" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="email" label="邮箱" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.email" placeholder="请输入邮箱" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="emergencyContact" label="紧急联系人" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.emergencyContact" placeholder="请输入紧急联系人" allowClear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%">
          <a-col :span="8">
            <a-form-item name="emergencyPhone" label="紧急联系电话" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.emergencyPhone" placeholder="请输入紧急联系电话" allowClear :maxlength="11" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item name="address" label="户籍/常住地址" :labelCol="labelColWide" :wrapperCol="wrapperColWide">
              <a-input v-model:value="model.address" placeholder="请输入户籍/常住地址" allowClear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%">
          <a-col :span="8">
            <a-form-item name="personCode" label="人员编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.personCode" placeholder="请输入人员编码" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="personType" label="人员类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-select v-model:value="model.personType" style="width: 100%" :options="personTypeOptions"
                placeholder="请选择人员类型" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="status" label="人员状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-select v-model:value="model.status" style="width: 100%" :options="personStatusOptions"
                placeholder="请选择人员状态" allowClear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%">
          <a-col :span="8">
            <a-form-item name="entryDate" label="入职日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-date-picker v-model:value="model.entryDate" value-format="YYYY-MM-DD" style="width: 100%"
                placeholder="请选择入职日期" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="leaveDate" label="离职日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-date-picker v-model:value="model.leaveDate" value-format="YYYY-MM-DD" style="width: 100%"
                placeholder="请选择离职日期" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item name="orderNo" label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input-number v-model:value="model.orderNo" style="width: 100%" placeholder="请输入排序码" :min="0" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </div>
  </XCModal>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';

import XCModal from '@/components/xuanchen/XCModal.vue';
import XCUploadImage from '@/components/xuanchen/XCUploadImage.vue';
import { validateIdCardApi, validateMobileApi, validateEmailApi, saveOrUpdate } from '../person.api';
import {
  genderOptions,
  maritalOptions,
  educationOptions,
  personTypeOptions,
  personStatusOptions,
} from '../person.data';
import type { PersonRecord, PersonSaveParams } from '../person.types';
import type { DeptRecord } from '../../dept/dept.types';
import { getDeptTreeApi } from '../../dept/dept.api';
import { getPostSelect } from '../../post/post.api';
import type { SelectOption } from '@/types/api';

/** 三列布局下的标签宽度 */
const labelCol = { span: 7 };
const wrapperCol = { span: 17 };
/** 跨两列（地址）时的标签宽度 */
const labelColWide = { span: 4 };
const wrapperColWide = { span: 20 };

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
})
const emit = defineEmits(['childOK']);

const validateIdCard = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateIdCardApi(String(model.id ?? ''), value);
}
const validateMobile = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateMobileApi(String(model.id ?? ''), value);
}
const validateEmail = async (_rule: Rule, value: string) => {
  if (!value) return;
  await validateEmailApi(String(model.id ?? ''), value);
}

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  idCard: [
    { pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, message: '请输入正确的身份证号', trigger: 'blur' },
    { validator: validateIdCard, trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    { validator: validateMobile, trigger: 'blur' }
  ],
  email: [
    { pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '请输入正确的邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  emergencyPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
}

const visible = ref(false);

/** 部门树 / 岗位下拉：首次打开弹窗时懒加载 */
const deptTree = ref<DeptRecord[]>([]);
const postOptions = ref<SelectOption[]>([]);
const optionsLoaded = ref(false);
const loadOptions = async () => {
  if (optionsLoaded.value) return;
  optionsLoaded.value = true;
  try {
    deptTree.value = await getDeptTreeApi();
  } catch {
    // 部门树加载失败不阻塞表单，允许仅维护人员基本信息
  }
  try {
    postOptions.value = await getPostSelect();
  } catch {
    // 岗位下拉加载失败不阻塞表单
  }
};

type IdList = Array<string | number> | undefined;
const model = reactive({
  id: '' as string,
  /** 部门IDS（sys_person_dept，随人员保存） */
  deptIds: undefined as IdList,
  /** 岗位IDS（sys_person_post，随人员保存） */
  postIds: undefined as IdList,
  /** 档案照片相对路径（sys_person.photo） */
  photo: '',
  realName: '',
  gender: undefined as number | undefined,
  birthday: undefined as string | undefined,
  idCard: '',
  nation: '',
  nativePlace: '',
  maritalStatus: undefined as number | undefined,
  education: undefined as number | undefined,
  personCode: '',
  mobile: '',
  email: '',
  emergencyContact: '',
  emergencyPhone: '',
  address: '',
  personType: undefined as number | undefined,
  status: undefined as number | undefined,
  entryDate: undefined as string | undefined,
  leaveDate: undefined as string | undefined,
  orderNo: undefined as number | undefined,
})

/** 照片上传契约：XCUploadImage 的 v-model 是「相对路径数组」，表单只存单个路径字符串 */
const photoFiles = computed<string[]>({
  get: () => (model.photo ? [model.photo] : []),
  set: (list) => {
    model.photo = list[0] ?? '';
  }
});

//打开弹窗
const add = async () => {
  visible.value = true;
  await loadOptions();
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.deptIds = [];
  model.postIds = [];
  model.photo = '';
  model.realName = '';
  model.gender = undefined;
  model.birthday = undefined;
  model.idCard = '';
  model.nation = '';
  model.nativePlace = '';
  model.maritalStatus = undefined;
  model.education = undefined;
  model.personCode = '';
  model.mobile = '';
  model.email = '';
  model.emergencyContact = '';
  model.emergencyPhone = '';
  model.address = '';
  model.personType = undefined;
  model.status = 1;
  model.entryDate = undefined;
  model.leaveDate = undefined;
  model.orderNo = undefined;
}
const edit = async (records: PersonRecord) => {
  visible.value = true;
  await loadOptions();
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id ?? '';
  model.deptIds = records.deptIds && records.deptIds.length > 0 ? [...records.deptIds] : [];
  model.postIds = records.postIds && records.postIds.length > 0 ? [...records.postIds] : [];
  model.photo = records.photo ?? '';
  model.realName = records.realName ?? '';
  model.gender = records.gender != null ? Number(records.gender) : undefined;
  model.birthday = records.birthday ?? undefined;
  model.idCard = records.idCard ?? '';
  model.nation = records.nation ?? '';
  model.nativePlace = records.nativePlace ?? '';
  model.maritalStatus = records.maritalStatus != null ? Number(records.maritalStatus) : undefined;
  model.education = records.education != null ? Number(records.education) : undefined;
  model.personCode = records.personCode ?? '';
  model.mobile = records.mobile ?? '';
  model.email = records.email ?? '';
  model.emergencyContact = records.emergencyContact ?? '';
  model.emergencyPhone = records.emergencyPhone ?? '';
  model.address = records.address ?? '';
  model.personType = records.personType != null ? Number(records.personType) : undefined;
  model.status = records.status != null ? Number(records.status) : undefined;
  model.entryDate = records.entryDate ?? undefined;
  model.leaveDate = records.leaveDate ?? undefined;
  model.orderNo = records.orderNo ?? undefined;
}

const handleOk = async () => {
  try {
    await rulesRef.value.validate();
  } catch {
    // 表单校验未通过：保留弹窗，等待用户修正
    return;
  }
  // deptIds/postIds 一并提交，由后端在同一事务内整组覆盖部门/岗位关系
  const params: PersonSaveParams = {
    ...model,
    deptIds: model.deptIds ?? [],
    postIds: model.postIds ?? [],
  };
  try {
    const res = await saveOrUpdate(params);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    emit('childOK');
    visible.value = false;
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示，此处仅阻止“成功提示+关窗”
  }
};

//子组件方法默认为私有
defineExpose({
  add,
  edit
})
</script>

<style scoped>
/* 三列布局下 8 行即可装满全部字段，正常屏幕不出现滚动条；极窄窗口保留纵向兜底滚动 */
.person-modal-body {
  max-height: 72vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 6px;
}

/* 全局 .modal-form-style 给表单项设了 margin:10px + width:100%，
   antd 的 inline 表单还会追加 margin-inline-end:16px，在 a-col 内横向多出的宽度
   会撑出弹窗底部的横向滚动条；这里把左右间距清零，只保留上下间距 */
.person-modal-body :deep(.modal-form-style .ant-form-item) {
  width: 100%;
  margin: 10px 0;
  margin-inline: 0;
}
</style>
