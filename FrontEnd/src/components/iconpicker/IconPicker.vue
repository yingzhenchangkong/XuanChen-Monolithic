<template>
  <a-popover :trigger="trigger" v-model:open="visible">
    <template #title>
      <a-input-search v-model:value="searchValue" placeholder="输入英文关键词进行搜索" @change="filterIcon" />
    </template>

    <template #content>
      <div class="icon-box">
        <div v-for="(item, index) in iconArr" :key="index" @click="handleClick(item)" class="icon-content"
          :style="{ background: icon === item ? '#268961' : '' }">
          <component :is="antIcons[item]" />
        </div>
      </div>
    </template>
    <slot name="iconSelect"></slot>
  </a-popover>
</template>

<script lang="ts">
import { defineComponent, ref, watch, getCurrentInstance } from 'vue'
import icons from './icon.json'
export default defineComponent({
  name: "IconPicker",
  props: {
    icon: {
      type: String,
      required: true
    },
    //自定义触发方式
    trigger: {
      type: String,
      default: 'click',
      validator: function (value: any) {
        return ['click', 'hover', 'focus'].indexOf(value) !== -1
      }
    }
  },
  setup(props, context) {
    const iconArr = ref<string[]>(icons)
    const visible = ref<boolean>(false)
    const searchValue = ref('')

    const handleClick = (icon: any) => {
      context.emit('update:icon', icon)
      visible.value = false;
    }

    /**
     * 进行搜索过滤
     */
    const filterIcon = () => {
      if (searchValue.value) {
        iconArr.value = icons.filter(item => item.toLowerCase().includes(searchValue.value.toLowerCase()))
      } else {
        iconArr.value = icons;
      }
    }

    watch(visible, () => {
      searchValue.value = ''
      iconArr.value = icons;
    })

    const instance = getCurrentInstance();
    const antIcons = instance?.appContext.config.globalProperties.$antIcons;

    return {
      visible,
      iconArr,
      handleClick,
      searchValue,
      filterIcon,
      antIcons
    }
  }
})
</script>

<style scoped>
.icon-box {
  overflow: auto;
  font-size: 20px;
  width: 250px;
  height: 230px;
  display: flex;
  flex-wrap: wrap;
  flex-direction: row;
  align-content: flex-start;
  justify-content: center;
}

.icon-content {
  width: 45px;
  height: 40px;
  margin: 5px;
  cursor: pointer;
  text-align: center;
  border-radius: 6px;
  border: 1px solid #ccc
}

.icon-content:hover {
  background: #1890ff;
}
</style>