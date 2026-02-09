# XuanChen-Monolithic

#### 介绍
```
基于SpringBoot3与Vue3技术栈构建的一套单体的前后端分离的企业级管理系统
```
#### 软件架构
```
* 环境：JDK17+
* 手机端：uniapp
* 后端：SpringBoot3、Mybatis Plus、shiro、druid、FastExcel
* 前端：Vue3、Ant Design Vue、vue-router、pinia、axios、dayjs、echarts、quill、vue3-slide-verify
* 数据库：MySQL8、Redis
* 即时通讯：WebSocket
```
#### 软件功能
```
|--登录
|--首页
|--顶部
|  |--通知提醒
|  |--头像
|  |  |--用户中心
|  |  |--修改密码
|  |  |--清除缓存
|  |  |--退出登录
|--系统管理
|  |--用户管理
|  |--角色管理
|  |--部门管理
|  |--岗位管理
|  |--菜单管理
|  |--字典管理
|  |--系统设置
|--系统监控
|  |--在线用户
|  |--性能监控
|  |--缓存监控
|  |--登录日志
|  |--操作日志
|--系统工具
|  |--代码生成器
|--通知公告
|  |--发布通知
|  |--通知管理
|  |--通知列表
|--更新中... ...
```
#### 目录结构
```
|--APP 手机端
|--BackEnd 后端
|  |--xuanchen-admin 后台入口
|  |--xuanchen-auth 认证、授权
|  |--xuanchen-common 通用模块
|  |  |--config 配置
|  |  |--constant 常量
|  |  |--entity 实体
|  |  |--fastexcel excel导入导出
|  |  |--server 服务类
|  |  |--utils 工具类
|  |--xuanchen-filemanage 文件管理
|  |--xuanchen-generator 代码生成器
|  |--xuanchen-monitor 监控模块
|  |--xuanchen-system 系统模块
|--FrontEnd 前端
|  |--assets 资源
|  |--components 组件
|  |--hooks 钩子
|  |--layout 布局
|  |--router 路由
|  |--stores 状态管理
|  |--utils 工具
|  |--views 视图
|  |  |--auth 权限
|  |  |--error 错误
|  |  |--monitor 监控
|  |  |--system 系统
|  |  |--tool 工具
|--Documents 文档
```