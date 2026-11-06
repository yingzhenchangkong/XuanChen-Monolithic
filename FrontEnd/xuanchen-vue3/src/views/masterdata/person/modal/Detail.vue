<template>
  <a-modal v-model:open="visible" title="人员详情" :width="1100" :footer="null">
    <div class="person-detail-body">
      <a-descriptions bordered size="small" :column="3" :label-style="{ width: '130px' }">
        <a-descriptions-item label="姓名">{{ text(model.realName) }}</a-descriptions-item>
        <a-descriptions-item label="照片">
          <a-image v-if="model.photo" :src="getImageView(model.photo as string) ?? undefined" :width="60"
            :style="{ borderRadius: '4px' }" />
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="性别">{{ mapText(genderMap, model.gender) }}</a-descriptions-item>
        <a-descriptions-item label="出生日期">{{ text(model.birthday) }}</a-descriptions-item>
        <a-descriptions-item label="身份证号">{{ text(model.idCard) }}</a-descriptions-item>
        <a-descriptions-item label="民族">{{ text(model.nation) }}</a-descriptions-item>
        <a-descriptions-item label="籍贯">{{ text(model.nativePlace) }}</a-descriptions-item>
        <a-descriptions-item label="婚姻状况">{{ mapText(maritalMap, model.maritalStatus) }}</a-descriptions-item>
        <a-descriptions-item label="学历">{{ mapText(educationMap, model.education) }}</a-descriptions-item>
        <a-descriptions-item label="人员编码">{{ text(model.personCode) }}</a-descriptions-item>
        <a-descriptions-item label="手机号">{{ text(model.mobile) }}</a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ text(model.email) }}</a-descriptions-item>
        <a-descriptions-item label="紧急联系人">{{ text(model.emergencyContact) }}</a-descriptions-item>
        <a-descriptions-item label="紧急联系电话">{{ text(model.emergencyPhone) }}</a-descriptions-item>
        <a-descriptions-item label="户籍/常住地址">{{ text(model.address) }}</a-descriptions-item>
        <a-descriptions-item label="部门">{{ text(model.deptNames) }}</a-descriptions-item>
        <a-descriptions-item label="岗位">{{ text(model.postNames) }}</a-descriptions-item>
        <a-descriptions-item label="人员类型">{{ typedText(personTypeMap, model.personType) }}</a-descriptions-item>
        <a-descriptions-item label="人员状态">{{ typedText(personStatusMap, model.status) }}</a-descriptions-item>
        <a-descriptions-item label="入职日期">{{ text(model.entryDate) }}</a-descriptions-item>
        <a-descriptions-item label="离职日期">{{ text(model.leaveDate) }}</a-descriptions-item>
        <a-descriptions-item label="排序码">{{ text(model.orderNo) }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ text(model.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ text(model.updateTime) }}</a-descriptions-item>
      </a-descriptions>
    </div>
    <div style="text-align: right; margin-top: 16px;">
      <a-button @click="handleClose">关闭</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import {
  educationMap,
  genderMap,
  maritalMap,
  personStatusMap,
  personTypeMap,
} from '../person.data';
import type { PersonRecord } from '../person.types';
import { getImageView } from '@/utils/ImageUtil';

const visible = ref(false);
const model = reactive<PersonRecord>({});

/** 空值统一显示为 - */
const text = (value: unknown) => (value === undefined || value === null || value === '' ? '-' : String(value));
/** 数值型字典（1男2女 等）转文本 */
const mapText = (map: Record<number, string>, value: unknown) => {
  if (value === undefined || value === null || value === '') return '-';
  return map[Number(value)] ?? String(value);
};
/** 带颜色的字典（人员类型 / 人员状态）转文本 */
const typedText = (map: Record<number, { text: string; color: string }>, value: unknown) => {
  if (value === undefined || value === null || value === '') return '-';
  return map[Number(value)]?.text ?? String(value);
};

/** 打开详情弹窗（部门/岗位名称由列表接口一并回传） */
const show = (records: PersonRecord) => {
  const target = model as Record<string, unknown>;
  Object.keys(target).forEach((key) => {
    delete target[key];
  });
  Object.assign(model, records);
  visible.value = true;
};

const handleClose = () => {
  visible.value = false;
};

defineExpose({ show });
</script>

<style scoped>
.person-detail-body {
  max-height: 72vh;
  overflow-y: auto;
  padding-right: 6px;
}
</style>
