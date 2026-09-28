# XuanChen（萱晨）管理系统介绍

## 一、项目概述

XuanChen 是一套**前后端分离的企业级中后台管理系统（脚手架）**，覆盖权限管理、组织架构、数据字典、通知推送、系统监控、代码生成等后台系统通用能力，可直接作为业务系统的底座进行二次开发。

仓库内**并存两套后端实现**，业务模块划分与接口契约基本一致、共用同一套前端，区别在于基础框架选型：

| | 后端实现 | 基础框架 |
|---|---|---|
| 旧版 | `BackEnd/xuanchen-springboot3` | Spring Boot 3 + Java 17 + Shiro |
| 新版（当前主线） | `BackEnd/xuanchen-springboot4` | Spring Boot 4 + Java 25 + Spring Security |

- **前端工程**：`FrontEnd/xuanchen-vue3`，基于 **Vue 3 + Vite 8 + TypeScript** 的单页应用（SPA），两套后端通用
- **手机端**：`APP/xuanchen-uniapp`，UniApp 工程
- **数据与中间件**：MySQL 8.0、Redis
- **通信方式**：HTTP/JSON（REST 风格）+ WebSocket（服务端实时推送）

整体链路：

```
浏览器 Vue3 SPA ──/api──▶ Vite Dev Proxy / Nginx ──▶ Spring Boot 多模块应用（3 或 4）
                                                      ├── MySQL 8（持久化）
                                                      ├── Redis（会话/限流/缓存）
                                                      └── WebSocket /ws（通知推送）
```

## 二、软件架构

```
* 环境：JDK17+
* 手机端：uniapp
* 后端：
* SpringBoot3版本：SpringBoot3、Mybatis Plus、Shiro、Druid、FastExcel
* SpringBoot4版本：SpringBoot4、Mybatis Plus、Spring Security、Fesod
* 前端：Vue3、Ant Design Vue、vue-router、pinia、axios、dayjs、echarts、quill
* 数据库：MySQL8、Redis
* 即时通讯：WebSocket
```

## 三、软件功能

> 以下内容按仓库 README.md 原文迁移，保持不变。

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

## 四、目录结构

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

## 五、后端工程 xuanchen-springboot3（旧版）

### 5.1 定位

SpringBoot3 版本是系统的**早期实现**，采用 Shiro 安全框架与 Druid 连接池，功能覆盖核心 RBAC、监控与代码生成，但认证形态与工程化能力相对精简。其模块划分、业务包结构与新版一致，接口契约保持兼容，同一套 Vue3 前端可直接对接。

### 5.2 多模块架构

父工程 `xuanchen-parent` 按职责拆分为 7 个模块：

| 模块 | 职责 |
|---|---|
| **xuanchen-admin** | 启动入口，聚合各业务模块、提供主类与 `application.yml` |
| **xuanchen-common** | 公共基座：统一返回体 `Result`、MyBatis-Plus 配置、AOP 切面（字典翻译/操作日志）、WebSocket 服务端、Redis/IP/字符串等工具 |
| **xuanchen-auth** | 认证授权：Shiro 配置、JWT 过滤器与 Realm、登录登出 |
| **xuanchen-system** | 核心业务：用户、角色、菜单、部门、岗位、字典、参数、通知及关联关系 |
| **xuanchen-filemanage** | 文件上传与静态资源访问 |
| **xuanchen-monitor** | 服务器、Redis 缓存、在线用户、登录日志、操作日志监控 |
| **xuanchen-generator** | 数据源管理、表/字段配置、Freemarker 模板代码生成 |

### 5.3 核心技术栈

- **Spring Boot 3 / Java 17**：Jakarta EE 命名空间，Maven 编译目标 17。
- **Apache Shiro（jakarta 版）+ java-jwt**：安全体系与新版的最大区别。`ShiroConfig` 以 `ShiroFilterFactoryBean` 装配过滤链——`/login`、文件静态路径、配置键查询、`/ws/**` 标记为 `anon` 匿名放行，其余路径统一走自定义 `JwtFilter`；`ShiroRealm` 负责令牌校验与授权信息加载，`JwtToken` 承载无状态令牌，另配 `CORSFilter` 处理跨域。
- **MyBatis-Plus**：单表 CRUD、分页、逻辑删除；复杂 SQL 走同名 Mapper XML。
- **Druid**：数据源连接池（`com.alibaba.druid.pool.DruidDataSource`），配置写于 `application-common.yml`。
- **MySQL 8 + Redis**：雪花算法字符串主键；Redis 承载会话与缓存。
- **WebSocket（JSR-356 原生）**：common 模块以 `@ServerEndpoint` 风格的 `WebSocketServer` + `WebSocketConfig` 实现，用于通知推送。
- **FastExcel**：Excel 导入导出，配套流式 `FastExcelListener`。
- **oshi-core**：服务器硬件与 JVM 指标采集。
- **Freemarker**：代码生成模板引擎。
- **fastjson2、Lombok**：序列化与样板代码精简。

### 5.4 功能边界

- **认证**：仅提供账号密码**登录 / 登出**；无注册、滑块验证码、邮箱验证码找回密码等能力。
- **系统管理**：用户、角色、菜单、部门、岗位、字典、参数、通知公告及用户—角色—部门—岗位关系，结构与新版一致。
- **系统监控**：服务器性能、缓存、在线用户、登录/操作日志（操作日志同样由 AOP 注解自动记录）。
- **代码生成器**：数据源管理 + 表配置 + 模板生成；此版本数据源**口令为明文入库**。
- **工程形态**：单一 `application-common.yml` 环境配置（无 dev/prod profile 分离）；公共模块只提供统一返回体 `Result`，**尚未建立全局异常处理器与业务异常体系**。

## 六、后端工程 xuanchen-springboot4（新版，当前主线）

### 6.1 定位

SpringBoot4 版本是系统的**当前主线**，在保持与旧版相同业务边界的基础上，升级到 Spring Boot 4 / Java 25，将安全框架由 Shiro 切换为 Spring Security，并补齐了认证安全、异常体系、配置隔离、数据源加密等工程化能力。

### 6.2 多模块架构

父工程 `xuanchen-parent` 按职责拆分为 7 个模块，依赖方向自上而下单向引用，web 能力由各模块按需声明，避免非 web 模块被拖入整套 MVC 与嵌入式容器：

| 模块 | 职责 |
|---|---|
| **xuanchen-admin** | 启动入口模块，聚合各业务模块、提供全局配置与主类 |
| **xuanchen-common** | 公共基座：统一返回体、全局异常、MyBatis-Plus 配置、AOP 切面、WebSocket、Redis/IP/密码等工具、多 profile 配置 |
| **xuanchen-auth** | 认证授权：Spring Security、JWT 过滤器、登录注册、滑块验证码、邮件验证码、找回密码 |
| **xuanchen-system** | 核心业务：用户、角色、菜单、部门、岗位、字典、参数、通知及各类关联关系 |
| **xuanchen-filemanage** | 文件上传、静态资源访问与文件安全校验 |
| **xuanchen-monitor** | 系统监控：服务器状态、Redis 缓存、在线用户、登录日志、操作日志 |
| **xuanchen-generator** | 代码生成器：数据源管理、表/字段配置、Freemarker 模板生成前后端 CRUD 代码 |

### 6.3 核心技术栈

- **Spring Boot / Java 25**：使用最新一代框架与语言特性，Lombok 在 JDK 25 下通过显式 `annotationProcessorPaths` 插桩。
- **Spring Security + JWT（java-jwt）**：无状态令牌认证。自实现 `JwtAuthenticationFilter` 从请求头 `XC-ACCESS-TOKEN` 解析会话，配合 `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler` 分别处理未认证（401）与无权限（403）；方法级用 `@PreAuthorize` 做角色控制。
- **MyBatis-Plus**（spring-boot4 专用 starter）：单表 CRUD、分页插件、逻辑删除（`@TableLogic`）自动接管；复杂查询（回收站、批量物理删除等）用同名 Mapper XML 编写 SQL，XML 与 Java 接口同包打包。
- **MySQL 8 + HikariCP**：约 20 张业务表，雪花算法字符串主键。
- **Redis**：存储登录会话、登录失败计数、限流计数、在线用户等；限流用 **Lua 脚本**实现 INCR+EXPIRE 原子固定窗口，消除 get-then-set 竞态。
- **WebSocket**：基于 Spring WebSocket，握手时经 `WsAuthBridge` 用 Redis 会话校验 token，用于通知公告的实时推送（事务提交后再推送，避免推了又回滚）。
- **邮件服务**：注册、忘记密码场景发送邮箱验证码。
- **fesod-sheet**：EasyExcel 进入 Apache 孵化后的继任者，支撑各模块 Excel 导入导出（流式读取监听器 `FesodSheetListener`）。
- **oshi-core**：采集服务器 CPU、内存、JVM、磁盘等指标。
- **Freemarker**：代码生成器模板引擎，产出 controller/service/mapper/entity/xml/vue 页面等成套代码。
- **fastjson2、Lombok**：序列化与样板代码精简。

### 6.4 功能模块

**认证与账户安全**

- 账号密码登录、注册、登出；登录页配**滑块验证码**（Java AWT 动态绘制背景/缺口图）
- 忘记密码：邮箱验证码校验后重置
- 管理员重置密码后标记"首次登录强制改密"
- 登录失败防护：按**用户名 + 来源 IP 双维度**计数锁定，配合滑块验证频率限制
- 密码 BCrypt 加密存储，新增/改密/重置统一执行复杂度策略（字母+数字+特殊字符，6–20 位）

**系统管理（RBAC 权限模型）**

- 用户管理、角色管理、菜单管理（菜单树驱动前端动态路由）、部门管理（树形）、岗位管理
- 用户—角色—部门—岗位多对多关系维护；角色授权菜单（事务保证"先删后插"一致性，自动补齐父级菜单）
- 数据字典：字典 + 字典项两级结构，后端通过 **AOP 切面 + `@Dict` 注解**对返回字段自动翻译
- 参数配置：系统级键值配置（如验证码开关）
- 通知公告：发布、管理、已读/未读状态、批量已读，并经 WebSocket 实时推送
- 回收站：列表数据逻辑删除入站，支持还原与彻底物理删除（物理删除时级联清理全部关系表）

**系统监控**

- 服务器监控：CPU、内存、JVM、磁盘实时指标
- 缓存监控：Redis 基本信息、按 key 浏览（按 STRING/LIST/SET/ZSET/HASH 类型分支读取大小与内容）
- 在线用户：查看与强制下线
- 登录日志、操作日志：操作日志通过 `@LogOperation` 注解 + AOP 自动落库，记录操作人、模块、参数、结果

**代码生成器**

- 多数据源连接管理：主机/端口/库名白名单校验（防止恶意库名注入 JDBC 连接属性）；密码使用 **AES-GCM 加密**入库、接口返回固定掩码
- 导入数据库表、配置字段的查询/编辑/必填/列表展示等 UI 行为
- 一键按 Freemarker 模板生成整套前后端 CRUD 代码

**文件管理**：图片等文件的本地上传与静态访问，含类型/路径安全校验。

### 6.5 后端工程实践

- 统一返回结构 `Result`（success/badRequest/unauthorized/conflict/error）与 `GlobalExceptionHandler` 全局兜底，业务异常用 `BusinessException` 携带业务码
- 多表写操作统一 `@Transactional(rollbackFor = Exception.class)`
- 标志位取值（启用/停用、正常/已删除）统一收敛到 `CommonConst` 常量，禁止裸字面量
- 配置分 `dev/prod` profile，口令等敏感项一律走环境变量注入，仓库内不内置真实口令

### 6.6 两套后端的主要差异对照

| 维度 | xuanchen-springboot3 | xuanchen-springboot4 |
|---|---|---|
| 框架 / JDK | Spring Boot3 / Java 17 | Spring Boot 4 / Java 25 |
| 安全框架 | Apache Shiro + 自定义 JwtFilter/Realm | Spring Security + 自定义认证过滤器 |
| 连接池 | Druid | HikariCP（Spring Boot 默认） |
| Excel | FastExcel | fesod-sheet（孵化继任者） |
| WebSocket | JSR-356 `@ServerEndpoint` | Spring WebSocket + 握手鉴权桥 |
| 认证功能 | 仅登录 / 登出 | 登录、注册、滑块验证码、邮箱找回、强制改密、失败锁定、密码策略 |
| 异常体系 | 仅 `Result`，无全局异常处理 | `GlobalExceptionHandler` + `BusinessException` |
| 数据源口令 | 明文入库 | AES-GCM 加密入库 + 接口掩码 |
| 限流 | 无 | Redis Lua 原子固定窗口 |
| 环境配置 | 单个 application-common.yml，口令明文 | dev/prod profile，环境变量注入 |
| 业务模块与前端 | 与新版一致，共用同一套 Vue3 前端 | 当前开发与修复主线 |

## 七、前端工程 xuanchen-vue3（两套后端通用）

### 7.1 技术栈与工程化

- **Vue 3**：全面采用 `<script setup>` 组合式 API
- **Vite 8**：开发服务器（`/api` 代理后端、WebSocket 代理改写 Origin）与生产构建
- **TypeScript**（精确锁版本，规避 vue-tsc 与 TS 7 的兼容问题），`vue-tsc` 全量类型检查纳入构建脚本
- **ant-design-vue**：UI 组件库；通过 **unplugin-vue-components + AntDesignVueResolver 按需自动引入**，不再全量注册组件与 700+ 图标，未使用代码可被 tree-shaking
- **Pinia**：组合式 store，管理 auth（会话）、menu（菜单）、tabs（页签）、websocket、collapsed（侧边栏折叠）五块状态
- **vue-router**：静态路由（登录/404）+ **按用户菜单权限动态注入的业务路由**，全局前置守卫统一处理鉴权、动态路由注册与 401 清理
- **axios**：统一实例与拦截器——请求自动带 token；响应统一弹错；blob 下载失败时自动反序列化 JSON 以显示后端真实消息
- **ECharts**：首页四个数据看板，组件卸载时 dispose 并移除 resize 监听防泄漏
- 其他：Quill 富文本（通知编辑）、dayjs、@ant-design/icons-vue、Sass

### 7.2 页面与功能

- **认证页**：登录、注册、忘记密码、滑块验证组件、强制修改密码页（注册/找回等页面仅在对接 SpringBoot4 后端时可用）
- **系统管理**：用户、角色（含分配菜单/用户）、菜单、部门（部门树+部门用户）、岗位（含分配用户）、字典（字典与字典项）、参数配置、通知公告（发布/管理/我的通知）
- **个人中心**：基本资料维护（含邮箱格式与唯一性校验）、头像上传、修改密码
- **系统监控**：服务器监控、缓存监控、在线用户、登录日志、操作日志、首页数据看板
- **开发工具**：数据源管理、代码生成（表配置、字段 UI 配置）
- 每个列表页统一遵循"列表 + 查询表单 + 新增编辑弹窗 + 回收站"的结构

### 7.3 前端工程沉淀

- **useList 通用 hook**：封装分页查询、增删、批量删除、选中行、查询/重置等列表通用逻辑，业务页只声明 URL 与查询参数
- **通用业务组件**：`XCModal`（弹窗）、`XCQueryForm`（查询表单）、`XCSelect`（字典/远程下拉）、`XCUploadImage`（图片上传）、`IconPicker`（菜单图标选择）
- **统一交互范式**：列表状态切换乐观更新（先翻 UI、失败回滚）；表单弹窗 await 校验 + 业务码判断，错误提示只弹一次
- **WebSocket 客户端**：应用层心跳看门狗、半开连接主动断开、指数退避重连（5s 起、封顶 60s）、单连接守卫防重复创建、生命周期日志仅开发环境输出
- **多环境构建**：`.env.dev/.env.test/.env.pro`，浏览器侧统一同源相对地址 `/api`，由 Nginx 反代，杜绝写死主机名导致的部署失败

## 八、系统特点小结

1. **双版本可对照演进**：同一业务模型分别在 Shiro/SpringBoot3 与 Spring Security/SpringBoot4 上落地，模块划分与接口契约一致，既是可运行系统，也是两套安全体系迁移升级的对照样本。
2. **权限闭环完整（新版）**：菜单级动态路由 + 接口级 `@PreAuthorize` + 角色菜单多对多授权，配登录失败锁定、验证码、密码策略形成多层安全防护。
3. **工程规范统一**：前后端均有明确的分层约定与可复用抽象（Result/异常体系、useList/通用组件、常量收敛、类型声明完备）。
4. **运维可观测**：操作/登录日志、在线用户、服务器与 Redis 监控齐备，通知支持 WebSocket 实时触达。
5. **可扩展的代码生成**：数据源加密托管 + 模板化生成，新增业务模块可低成本产出标准 CRUD。
6. **技术栈新**：新版采用 Spring Boot 4 + Java 25、Vite 8、Vue 3、ant-design-vue，适合作为现代 Java/Vue 技术栈的学习与二开底座；旧版则为 JDK 17 保守环境提供兼容选择。
