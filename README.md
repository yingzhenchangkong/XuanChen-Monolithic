# XuanChen-Monolithic

#### 介绍
```
一套基于 SpringBoot3/SpringBoot4、Vue3 及 UniApp 技术栈构建的单体式前后端分离企业级管理系统。
```
#### 软件架构
```
* 环境：JDK17+
* 手机端：uniapp
* 后端：
* SpringBoot3版本：SpringBoot3、Mybatis Plus、Shiro、Druid、FastExcel
* SpringBoot4版本：SpringBoot4、Mybatis Plus、Spring Security、Fesod
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
|  |--xuanchen-uniapp uniapp版本
|--BackEnd 后端
|  |--xuanchen-springboot3 SpringBoot3版本
|  |  |--xuanchen-admin 后台入口
|  |  |--xuanchen-auth 认证、授权
|  |  |--xuanchen-common 通用模块
|  |  |  |--config 配置
|  |  |  |--constant 常量
|  |  |  |--entity 实体
|  |  |  |--fastexcel excel导入导出
|  |  |  |--server 服务类
|  |  |  |--utils 工具类
|  |  |--xuanchen-filemanage 文件管理
|  |  |--xuanchen-generator 代码生成器
|  |  |--xuanchen-monitor 监控模块
|  |  |--xuanchen-system 系统模块
|  |--xuanchen-springboot4 SpringBoot4版本
|  |  |--xuanchen-auth 认证、授权
|  |  |--xuanchen-common 通用模块
|  |  |  |--config 配置
|  |  |  |--constant 常量
|  |  |  |--entity 实体
|  |  |  |--server 服务类
|  |  |  |--utils 工具类
|  |  |--xuanchen-system 后台入口、系统模块
|--FrontEnd 前端
|  |--xuanchen-vue3 vue3版本
|  |  |--assets 资源
|  |  |--components 组件
|  |  |--hooks 钩子
|  |  |--layout 布局
|  |  |--router 路由
|  |  |--stores 状态管理
|  |  |--utils 工具
|  |  |--views 视图
|  |  |  |--auth 权限
|  |  |  |--error 错误
|  |  |  |--monitor 监控
|  |  |  |--system 系统
|  |  |  |--tool 工具
|--Documents 文档
|  |--xuanchen-monolithic.sql 数据库脚本
```