<template>
  <a-row :gutter="20">
    <a-col :span="5">
      <UserAvatar />
    </a-col>
    <a-col :span="10">
      <UserInfo />
    </a-col>
    <a-col :span="9">
      <ChangePassword />
    </a-col>
  </a-row>
</template>
<script setup lang="ts">
import ChangePassword from './modules/ChangePassword.vue';
import UserAvatar from './modules/UserAvatar.vue';
import UserInfo from './modules/UserInfo.vue';
</script>