import { defineAsyncComponent, h, type Component } from 'vue';

/**
 * 菜单图标（数据库 sys_menu.icon 中的字符串，如 "SettingOutlined"）通过
 * <component :is="meta.icon"> 动态渲染，模板编译期无法确定具体组件，
 * 不能走 unplugin-vue-components 的静态按需解析，也不能为它们全量注册 700+ 图标。
 *
 * 这里在图标真正渲染时才异步加载 @ant-design/icons-vue 整个桶模块：
 * 它会被拆成独立异步 chunk，登录后进入布局才下载，不阻塞首屏；
 * 同时“菜单管理”的图标选择器里任选图标都能正常渲染。
 */
const iconCache = new Map<string, Component>();

export function resolveMenuIcon(name?: string): Component | null {
  if (!name) {
    return null;
  }
  const cached = iconCache.get(name);
  if (cached) {
    return cached;
  }
  const asyncIcon = defineAsyncComponent({
    loader: () =>
      // 全量图标经 vite 虚拟模块聚合并被拆到独立异步 chunk（见 vite.config.ts）
      import('virtual:ant-icons-all').then((mod) => {
        const icon = mod.icons[name];
        // 数据库中配置了已下线/不存在的图标名时渲染空节点，避免异步组件 reject 导致整页报错
        return icon || { name: 'UnknownMenuIcon', render: () => h('span') };
      }),
    // 菜单图标体积小、随布局必然立即使用，不展示 loading 占位，避免闪烁
    delay: 0
  });
  iconCache.set(name, asyncIcon);
  return asyncIcon;
}
