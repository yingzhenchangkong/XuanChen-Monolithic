<template>
  <a-popover :trigger="trigger" v-model:open="visible">
    <template #content>
      <div class="picker-header">
        <div class="picker-controls">
          <a-radio-group v-model:value="activeStyle" size="small" class="style-radio">
            <a-radio v-for="s in STYLES" :key="s.value" :value="s.value">{{ s.label }}</a-radio>
          </a-radio-group>
          <a-input-search v-model:value="searchValue" class="picker-search" enter-button allow-clear
            placeholder="输入英文关键词进行搜索" @change="filterIcon" />
        </div>
      </div>

      <!-- 六大语义分类做 tab，分类内按官网顺序排列 -->
      <a-tabs v-model:activeKey="activeCategory" size="small" class="icon-tabs">
        <a-tab-pane v-for="category in ICON_CATEGORY_ORDER" :key="category" :tab="category">
          <div class="icon-box">
            <div v-for="item in currentIcons" :key="item" class="icon-content"
              :style="{ background: icon === item ? '#268961' : '' }" @click="handleClick(item)">
              <component :is="antIcons[item]" />
            </div>
            <div v-if="currentIcons.length === 0" class="icon-empty">无匹配图标</div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </template>
    <slot name="iconSelect"></slot>
  </a-popover>
</template>

<script lang="ts">
import { computed, defineComponent, ref, watch } from 'vue'
// 图标选择器本身就需要枚举全部图标，这里引用 vite 虚拟聚合模块（深度路径生成），
// 它只被“菜单管理”页面使用，随路由懒加载打进异步 chunk，不进主包
import { icons as antIcons } from 'virtual:ant-icons-all'
import icons from './icon.json'
import { ICON_BASE_ORDER, ICON_CATEGORY_ORDER, categoryOf, type IconCategory } from './iconCategories'

type Style = 'Outlined' | 'Filled' | 'TwoTone'
/** 风格单选的取值与展示文案（对照官网：线框 / 实底 / 双色） */
const STYLES: { value: Style; label: string }[] = [
  { value: 'Outlined', label: '线框风格' },
  { value: 'Filled', label: '实底风格' },
  { value: 'TwoTone', label: '双色风格' },
]
const STYLE_SUFFIX = /(Outlined|Filled|TwoTone)$/

interface IconSection {
  category: IconCategory
  icons: string[]
}
type Sections = Record<Style, IconSection[]>

/**
 * 基名 -> 官方展示位次（分类序 * 100000 + 分类内序），用于让同一分类内的图标
 * 按 antd 官网的顺序排列（官网是 StepBackward → StepForward → …，比字母序更符合直觉）。
 */
const SORT_INDEX: Record<string, number> = (() => {
  const index: Record<string, number> = {}
  ICON_CATEGORY_ORDER.forEach((category, categoryIndex) => {
    ICON_BASE_ORDER[category].forEach((base, baseIndex) => {
      index[base] = categoryIndex * 100000 + baseIndex
    })
  })
  return index
})()

const sortIndexOf = (name: string): number =>
  SORT_INDEX[name.replace(STYLE_SUFFIX, '')] ?? Number.MAX_SAFE_INTEGER

/**
 * 把扁平清单拆成「风格 -> 语义分类分组」两层。
 *
 * 实现上先从传入清单归集、再按分类归组（而不是拿分类表逐条去取），
 * 这样即使分类表漏了某个名字，它也只会落到兜底分类而已，绝不会从界面上消失。
 * 分类顺序固定为官网六大分类，分类内再按官网顺序排序。
 */
function groupIcons(list: string[]): Sections {
  const sections: Sections = { Outlined: [], Filled: [], TwoTone: [] }

  for (const style of STYLES.map(s => s.value)) {
    const buckets = new Map<IconCategory, string[]>()
    for (const name of list) {
      if (name.match(STYLE_SUFFIX)?.[1] !== style) continue
      const category = categoryOf(name)
      const bucket = buckets.get(category)
      if (bucket) bucket.push(name)
      else buckets.set(category, [name])
    }

    for (const category of ICON_CATEGORY_ORDER) {
      const bucket = buckets.get(category)
      if (bucket?.length) {
        sections[style].push({
          category,
          icons: bucket.sort((a, b) => sortIndexOf(a) - sortIndexOf(b)),
        })
      }
    }
  }

  return sections
}

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
    const groups = ref<Sections>(groupIcons(icons))
    const activeStyle = ref<Style>('Outlined')
    const activeCategory = ref<IconCategory>(ICON_CATEGORY_ORDER[0])
    const visible = ref<boolean>(false)
    const searchValue = ref('')

    /** 当前风格下每个分类的图标数，用于搜索后自动跳转空分类的判断 */
    const styleTotal = computed<Record<Style, number>>(() => {
      const total = { Outlined: 0, Filled: 0, TwoTone: 0 }
      for (const { value } of STYLES) {
        total[value] = groups.value[value].reduce((sum, section) => sum + section.icons.length, 0)
      }
      return total
    })

    /** 当前 tab（风格 + 分类）应展示的图标清单 */
    const currentIcons = computed<string[]>(() => {
      const section = groups.value[activeStyle.value].find(s => s.category === activeCategory.value)
      return section ? section.icons : []
    })

    const handleClick = (icon: any) => {
      context.emit('update:icon', icon)
      visible.value = false;
    }

    /**
     * 进行搜索过滤：先按关键词筛出扁平清单，再重新分组，保证分类 tab 的数量随之更新。
     * 跳转顺序：当前风格被筛空 -> 切到第一个还有结果的风格；
     *           当前分类在（新的）风格下没有结果 -> 切到该风格第一个有结果的分类；
     *           全部为空时保持不动，交由空态文案提示。
     */
    const filterIcon = () => {
      const keyword = searchValue.value.trim().toLowerCase()
      const matched = keyword ? icons.filter(item => item.toLowerCase().includes(keyword)) : icons
      groups.value = groupIcons(matched)

      if (styleTotal.value[activeStyle.value] === 0) {
        const nextStyle = STYLES.map(s => s.value).find(style => styleTotal.value[style] > 0)
        if (nextStyle) activeStyle.value = nextStyle
      }

      const sections = groups.value[activeStyle.value]
      if (sections.length && !sections.some(s => s.category === activeCategory.value)) {
        activeCategory.value = sections[0].category
      }
    }

    watch(visible, () => {
      searchValue.value = ''
      groups.value = groupIcons(icons)
      // 保留用户上次停留的风格与分类，避免每次打开都被拽回初始位置
    })

    return {
      visible,
      groups,
      activeStyle,
      activeCategory,
      currentIcons,
      styleTotal,
      handleClick,
      searchValue,
      filterIcon,
      antIcons,
      STYLES,
      ICON_CATEGORY_ORDER
    }
  }
})
</script>

<style scoped>
/* 宽度由 --icon-picker-width 统一控制（.icon-box / .icon-tabs 继承同一变量）。
   图标为原始 2/3 后单格行距 = 30 + 左右各 3 = 36px，480px 去掉约 15px 滚动条后可用 465px
   → 每行 12 个，最后一行不足时靠左对齐。改图标尺寸时记得同步这个值 */
.picker-header {
  --icon-picker-width: 480px;
  width: var(--icon-picker-width);
}

.picker-label {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

/* 风格单选 + 搜索同一行：单选按官网样式做成连体描边按钮组，搜索占满剩余宽度 */
.picker-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.style-radio {
  flex: none;
  white-space: nowrap;
}

.style-radio :deep(.ant-radio-wrapper) {
  display: inline-flex;
  align-items: center;
  /* 高度与 .picker-search（antd 默认 32px 输入框）严格对齐 */
  height: 32px;
  box-sizing: border-box;
  margin: 0;
  padding: 0 10px;
  font-size: 12px;
  background: #fff;
  border: 1px solid #d9d9d9;
}

.style-radio :deep(.ant-radio-wrapper:first-child) {
  border-radius: 4px 0 0 4px;
}

.style-radio :deep(.ant-radio-wrapper:last-child) {
  border-radius: 0 4px 4px 0;
}

/* 相邻单选共享边框，形成截图中的连体按钮组 */
.style-radio :deep(.ant-radio-wrapper + .ant-radio-wrapper) {
  border-left: none;
}

.picker-search {
  flex: 1;
  min-width: 0;
}

/* 六个分类 tab 等宽铺满：压缩字号与内边距，保证 470px 内不溢出成「更多」下拉 */
.icon-tabs {
  width: var(--icon-picker-width, 480px);
  margin-top: 8px;
}

.icon-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 4px;
}

.icon-tabs :deep(.ant-tabs-tab) {
  /* 470px 内放下 6 个中文 tab：32 字 * 12px + 6 * 10px 内边距 = 444px，留 26px 余量防溢出成「更多」 */
  padding: 4px 5px;
  margin: 0 !important;
}

.icon-tabs :deep(.ant-tabs-tab-btn) {
  font-size: 12px;
}

.icon-box {
  overflow: auto;
  width: var(--icon-picker-width, 480px);
  height: 300px;
  display: flex;
  flex-wrap: wrap;
  flex-direction: row;
  align-content: flex-start;
  /* 图标靠左对齐（不因最后一行数量不足而居中） */
  justify-content: flex-start;
}

.icon-empty {
  flex: 0 0 100%;
  padding: 24px 0;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  text-align: center;
}

.icon-content {
  /* 图标为原始尺寸的 2/3：字号 20 → 13px，格子 45×40 → 30×26（等比），margin 5 → 3，圆角 6 → 4 */
  width: 30px;
  height: 26px;
  margin: 3px;
  font-size: 13px;
  cursor: pointer;
  text-align: center;
  border-radius: 4px;
  border: 1px solid #ccc
}

.icon-content:hover {
  background: #1890ff;
}
</style>
