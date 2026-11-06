import { createRouter, createWebHistory } from 'vue-router';
import { markRaw } from 'vue';
import Layout from '@/layout/Index.vue';

export const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/Login.vue'),
    hidden: true,
  },
  {
    // 首次登录/被重置密码后的强制改密页：独立于 Layout，不依赖菜单等业务接口
    path: '/force-change-password',
    name: 'forceChangePassword',
    component: () => import('@/views/auth/ForceChangePassword.vue'),
    hidden: true,
  },
  {
    // 自助注册页：独立于 Layout，无需登录会话（已加入路由守卫白名单）
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/Register.vue'),
    hidden: true,
  },
  {
    // 文件分享公开页：凭分享编号访问，免登录（对应后端 /share/** 匿名白名单）
    path: '/share/:no',
    name: 'shareView',
    component: () => import('@/views/filemanage/share/ShareView.vue'),
    meta: {
      title: '文件分享',
    },
    hidden: true,
  },
  {
    path: '/system/usercenter',
    component: markRaw(Layout),
    children: [
      {
        path: '',
        name: 'userCenter',
        component: () => import('@/views/system/usercenter/Index.vue'),
        meta: {
          title: '用户中心',
        },
        hidden: true,
      },
    ]
  },
  {
    // 报表查看页：按报表编码访问（/report/viewer/:code），隐藏菜单（静态路由）
    path: '/report/viewer/:code',
    component: markRaw(Layout),
    children: [
      {
        path: '',
        name: 'reportViewer',
        component: () => import('@/views/report/viewer/ReportViewer.vue'),
        meta: {
          title: '报表查看',
        },
        hidden: true,
      },
    ]
  },
  {
    // 大屏设计器：全屏独立页面（不包 Layout），由设计画廊进入
    path: '/visualization/designer/:id',
    name: 'visualizationDesigner',
    component: () => import('@/views/visualization/design/VisualizationDesigner.vue'),
    meta: {
      title: '大屏设计',
    },
    hidden: true,
  },
  {
    // 在线表单设计器：全屏独立页面（不包 Layout），由在线表单列表进入
    path: '/businessprocess/form/design/:id',
    name: 'businessprocessFormDesigner',
    component: () => import('@/views/businessprocess/form/FormDesigner.vue'),
    meta: {
      title: '表单设计',
    },
    hidden: true,
  },
  {
    // 报表模板设计器：全屏独立页面（不包 Layout），由报表模板列表的「设计」进入
    path: '/report/template/design/:id',
    name: 'reportTemplateDesigner',
    component: () => import('@/views/report/template/design/TemplateDesignPage.vue'),
    meta: {
      title: '模板设计',
    },
    hidden: true,
  },
  {
    // 代码生成配置：全屏独立页面（不包 Layout），由代码生成列表的「新增」进入
    path: '/generator/design',
    name: 'generatorDesign',
    component: () => import('@/views/generator/gentable/design/GenTableDesignPage.vue'),
    meta: {
      title: '新增代码生成',
    },
    hidden: true,
  },
  {
    // BPMN 流程设计器：全屏独立页面（不包 Layout），由流程配置列表进入
    path: '/businessprocess/designer/:id',
    name: 'businessprocessDesigner',
    component: () => import('@/views/businessprocess/design/BusinessProcessDesigner.vue'),
    meta: {
      title: '流程设计',
    },
    hidden: true,
  },
  {
    // 思维导图编辑器：全屏独立页面（不包 Layout），由在线文档列表进入
    path: '/onlinedoc/mindmap/edit/:id',
    name: 'mindmapEditor',
    component: () => import('@/views/online/mindmap/MindMapEditor.vue'),
    meta: {
      title: '思维导图编辑',
    },
    hidden: true,
  },
  {
    // 流程图/架构图编辑器：全屏独立页面（不包 Layout），由在线文档列表进入
    path: '/onlinedoc/diagram/edit/:id',
    name: 'diagramEditor',
    component: () => import('@/views/online/diagram/DiagramEditor.vue'),
    meta: {
      title: '流程图编辑',
    },
    hidden: true,
  },
  {
    // ONLYOFFICE 协同编辑：全屏独立页面，由「我的文件」进入
    path: '/fileedit/:id',
    name: 'officeEditor',
    component: () => import('@/views/filemanage/OfficeEditor.vue'),
    meta: {
      title: '在线协同编辑',
    },
    hidden: true,
  },
  {
    // 数据接口编辑器：全屏独立页面（不包 Layout），由接口管理列表进入
    path: '/openapi/design/:id',
    name: 'apiEditor',
    component: () => import('@/views/api/designer/ApiEditor.vue'),
    meta: {
      title: '接口编辑',
    },
    hidden: true,
  },
  {
    // 大屏展示页：按大屏编码访问（/visualization/viewer/:code），全屏铺满
    path: '/visualization/viewer/:code',
    name: 'visualizationViewer',
    component: () => import('@/views/visualization/viewer/VisualizationViewer.vue'),
    meta: {
      title: '数据大屏',
    },
    hidden: true,
  },
]

/**
 * 404 兜底路由：不能随初始路由一起注册。
 * 刷新内页时首跳发生在动态路由注入之前，若 catch-all 先注册，配合守卫中
 * “注入后重导航”链路之外的边界场景会被错误命中；必须在权限守卫 addRoute
 * 完所有动态路由之后再追加，保证具体路由优先于 catch-all 匹配。
 */
export const notFoundRoute = {
  path: '/:pathMatch(.*)*',
  name: '404',
  component: () => import('@/views/error/404.vue'),
  hidden: true,
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes,
})

export default router
