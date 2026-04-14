<template>
  <a-result status="404" title="404" sub-title="非常抱歉，您访问的页面走丢了~~">
      <template #extra>
        <a-button type="primary" @click="$router.push('/')">返回首页</a-button>
      </template>
    </a-result>
</template>