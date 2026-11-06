# XuanChen（萱晨）管理系统介绍

## 一、项目概述

XuanChen 是一套**前后端分离的企业级中后台开发平台（脚手架）**，除权限管理、组织架构、数据字典、通知推送、系统监控、代码生成等通用底座能力外，还内置了一批开箱即用的业务模块：**审批流程（自研 BPM 引擎）、文件中心（在线预览）、协同 Office 文档、报表中心、数据可视化大屏、接口服务（API 编排与开放分发）、AI 助手与知识库（RAG）**，可直接作为业务系统的底座进行二次开发。

数据库脚本 `Documents/xuanchen-monolithic.sql` 共包含 54 张业务表，主数据表统一采用模块化前缀（`sys_` 系统账号、`md_` 主数据、`bpm_` 审批流、`file_` 文件、`online_` 在线文档、`rpt_` 报表、`vis_` 大屏、`api_` 接口、`gen_` 代码生成、`log_` 日志）。

仓库内**并存两套后端实现**，业务模块划分与接口契约基本一致、共用同一套前端，区别在于基础框架选型：

| | 后端实现 | 基础框架 |
|---|---|---|
| 旧版 | `BackEnd/xuanchen-springboot3` | Spring Boot 3 + Java 17 + Shiro |
| 新版（当前主线） | `BackEnd/xuanchen-springboot4` | Spring Boot 4 + Java 25 + Spring Security |

- **前端工程**：`FrontEnd/xuanchen-vue3`，基于 **Vue 3 + Vite 8 + TypeScript** 的单页应用（SPA），两套后端通用
- **手机端**：`APP/xuanchen-uniapp`，UniApp 工程
- **数据与中间件**：MySQL 8.0、Redis、MinIO 对象存储（文件中心）、PostgreSQL + pgvector（AI 知识库，可选）
- **通信方式**：HTTP/JSON（REST 风格）+ WebSocket（通知 / 审批任务 / 催办 / 抄送实时推送）

整体链路：

```
浏览器 Vue3 SPA ──/api──▶ Vite Dev Proxy / Nginx ──▶ Spring Boot 多模块应用（3 或 4）
                                                      ├── MySQL 8（持久化）
                                                      ├── Redis（会话/限流/缓存/分片上传）
                                                      ├── MinIO（文件对象存储）
                                                      └── WebSocket /ws（通知与审批实时推送）
```

## 二、软件架构

```
* 环境：JDK17+（新版主线为 JDK25）
* 手机端：uniapp
* 后端：
* SpringBoot3版本：SpringBoot3、Mybatis Plus、Shiro、Druid、fesod-sheet、LangChain4j
* SpringBoot4版本：SpringBoot4、Mybatis Plus、Spring Security、Druid、fesod-sheet、LangChain4j
* 前端：Vue3、Ant Design Vue、vue-router、pinia、axios、dayjs、echarts、quill、bpmn-js
* 中间件：MySQL8、Redis、MinIO、ONLYOFFICE DocumentServer（在线文档协同，可选）、Ollama（AI 推理，可选）
* 即时通讯：WebSocket
```

## 三、软件功能

> 菜单分组与页面名称以实际运行系统为准（见[第八节系统功能截图](#八系统功能截图)）。

```
|--登录 / 注册 / 忘记密码（滑块验证码、邮箱验证码、强制改密）
|--首页（欢迎语 / 未读公告 / 我的待办 / 今日登录 / 快捷入口）
|--顶部
|  |--通知提醒（WebSocket 实时推送）
|  |--多页签工作区
|  |--头像
|  |  |--用户中心
|  |  |--修改密码
|  |  |--清除缓存
|  |  |--退出登录
|  |--AI 助手（右下角悬浮对话）
|--基础数据
|  |--人员管理（人员档案、导入导出、部门/岗位分配）
|  |--部门管理（树形、部门负责人）
|  |--岗位管理
|--系统管理
|  |--账号管理（登录凭证、角色分配）
|  |--角色管理（分配菜单 / 用户）
|  |--菜单管理
|  |--字典管理
|  |--参数配置
|  |--数据源配置
|--系统监控
|  |--在线用户（强制下线）
|  |--性能监控（CPU / 内存 / JVM / 磁盘）
|  |--缓存监控（Redis）
|  |--登录日志 / 操作日志 / 接口日志 / 代码生成日志
|--文件中心
|  |--我的文件（文件夹、上传/秒传/分片续传、在线预览：图片/文本/CAD/Office）
|  |--回收站 / 配额管理 / 文件分享 / 协同编辑
|--流程审批
|  |--在线表单（动态表单设计）
|  |--流程配置（bpmn-js 可视化设计器）
|  |--发起申请 / 待我审批 / 我的申请 / 我已审批
|--在线文档
|  |--协同文档（在线 Word / Excel / PPT 多人共编）
|  |--思维导图 / 流程图
|--报表管理（模板设计 / 报表列表 / 报表查看）
|--接口服务（接口管理 / 采集数据 / 接口日志 / 对外分发）
|--数据大屏（大屏设计 / 大屏展示）
|--待办事项（审批任务与通知统一待办）
|--通知公告（发布通知 / 通知管理 / 通知列表）
|--代码生成器（项目预设 / 代码生成 / 生成日志）
|--AI 知识库（知识库管理 / 检索测试）
|--回收站（各业务模块通用：还原 / 彻底删除）
```

## 四、目录结构

```
|--APP 手机端
|  |--xuanchen-uniapp uniapp版本（首页/系统/监控/通知/大屏/用户）
|--BackEnd 后端（两套实现，模块划分一致）
|  |--xuanchen-springboot3 SpringBoot3版本
|  |--xuanchen-springboot4 SpringBoot4版本
|  |  |--xuanchen-boot 启动入口
|  |  |--xuanchen-auth 认证、授权
|  |  |--xuanchen-common 通用模块（返回体/全局异常/AOP/Excel/WebSocket/工具）
|  |  |--xuanchen-system 系统模块（账号、角色、菜单、字典、参数、通知、待办）
|  |  |--xuanchen-masterdata 主数据模块（部门、人员、岗位及人员-部门/岗位关系）
|  |  |--xuanchen-monitor 监控模块（服务器/缓存/在线用户/登录日志/操作日志/接口日志/生成日志）
|  |  |--xuanchen-filemanage 文件中心（文件/文件夹/分享/配额/回收站/在线Office协同）
|  |  |--xuanchen-businessprocess 审批流程（自研BPM引擎 + bpmn-js设计器）
|  |  |--xuanchen-online 在线协同文档（在线Word/Excel/PPT、绘图、思维导图）
|  |  |--xuanchen-report 报表中心（数据源/数据集/报表/模板）
|  |  |--xuanchen-visualization 数据可视化大屏
|  |  |--xuanchen-api 接口服务（API编排/调度/日志/对外分发）
|  |  |--xuanchen-ai AI助手与知识库（LangChain4j + Ollama + pgvector）
|  |  |--xuanchen-generator 代码生成器
|--FrontEnd 前端
|  |--xuanchen-vue3 vue3版本
|  |  |--assets 资源
|  |  |--components 组件（XC通用组件 + 审批流组件）
|  |  |--hooks 钩子（useList / useDragSort / useStatusToggle）
|  |  |--layout 布局
|  |  |--router 路由
|  |  |--stores 状态管理（auth/menu/tabs/websocket/collapsed）
|  |  |--utils 工具
|  |  |--views 视图
|  |  |  |--system 系统管理
|  |  |  |--masterdata 主数据管理（部门/人员/岗位）
|  |  |  |--monitor 系统监控
|  |  |  |--generator 开发工具
|  |  |  |--businessprocess 审批流程
|  |  |  |--filemanage 文件中心
|  |  |  |--online 协同文档
|  |  |  |--report 报表中心
|  |  |  |--visualization 可视化大屏
|  |  |  |--api 接口服务
|  |  |  |--ai AI助手
|  |  |  |--auth 登录注册 / error 错误页
|--Documents 文档
|  |--xuanchen-monolithic.sql 数据库脚本（54 张表全量 dump）
|  |--docker-compose.yml 本地中间件编排
|  |--系统截图/ 53 张系统功能截图（见第八节）
|  |--前端目录结构说明.md / 知识库说明.md 专题文档
```

## 五、后端工程（两套实现的公共描述）

两套后端均为 Maven 多模块工程，父工程 `xuanchen-parent` 按职责拆分为 **14 个模块**，模块划分与接口契约保持一致，同一套 Vue3 前端可直接切换对接：

| 模块 | 职责 |
|---|---|
| **xuanchen-boot** | 启动入口，聚合各业务模块、提供主类与 `application.yml` |
| **xuanchen-common** | 公共基座：统一返回体 `Result`、全局异常 `GlobalExceptionHandler` + `BusinessException`、MyBatis-Plus 配置、AOP 切面（字典翻译/操作日志）、WebSocket 服务端、fesod Excel 导入导出、Redis/IP/密码等工具 |
| **xuanchen-auth** | 认证授权：登录登出、注册、滑块验证码、邮箱验证码找回密码、JWT 过滤器（sb3 为 Shiro Realm，sb4 为 Spring Security 过滤链） |
| **xuanchen-system** | 核心业务：账号 `sys_account`、角色、菜单、字典、参数、通知、待办 `sys_todo`、首页看板；单向依赖 masterdata 完成账号绑定人员的存在性校验 |
| **xuanchen-masterdata** | 主数据：部门 `md_dept`（部门负责人）、人员 `md_person`（档案/导入导出/回收站）、岗位 `md_post`、人员—部门 `md_person_dept` 与人员—岗位 `md_person_post` 关系；对外 REST 保持 `/system/dept`、`/system/person`、`/system/post` 等不变 |
| **xuanchen-monitor** | 服务器、Redis 缓存、在线用户（强制下线）、登录日志、操作日志、接口日志、代码生成日志 |
| **xuanchen-filemanage** | 文件中心：文件/文件夹、MD5 秒传、分片上传（Redis 管理分块，TTL 24h）、文件分享、配额、回收站、类型白名单/魔数校验；存储抽象 `StorageService`（本地 / MinIO 双实现）；ONLYOFFICE 在线 Office 协同（JWT 回调鉴权、协作者权限） |
| **xuanchen-businessprocess** | 审批流程：**自研 BPM 引擎**（非 Flowable）+ 前端 bpmn-js 设计器；BPMN2.0 XML 解析（JDK DOM，零第三方依赖）+ `xc` 扩展命名空间；多 token 推进（并行网关分叉/汇聚、token 状态 JSON 持久化、行锁保证并发正确）、或签/会签、SpEL 条件表达式、审批人规则（用户/角色/自选/部门负责人/直属上级 N 级/表单字段）、转办/驳回/催办/抄送/委托，任务同步待办并经 WebSocket 实时推送 |
| **xuanchen-online** | 在线协同文档：在线 Word/Excel/PPT（POI 生成空白文档落库文件中心，编辑复用 ONLYOFFICE），协作者表 `file_collaborator`（可编辑/只读）；在线绘图、思维导图 |
| **xuanchen-report** | 报表中心：多数据源（口令 AES 加密入库）、数据集（`SqlGuard` 仅允许单条 SELECT 防注入）、报表模板与报表设计/查看 |
| **xuanchen-visualization** | 数据可视化大屏：大屏 `vis_screen`、数据集 `vis_dataset`（SQL 只许单条 SELECT，max-rows=2000），前端全屏设计器与展示页 |
| **xuanchen-api** | 接口服务：API 定义/编排引擎（步骤定义 `StepDef` + 模板求值 `TemplateEval`）、模拟数据 `api_data`、定时调度、调用日志 `api_log`，对外公开分发入口 `/open/api/{apiCode}`（匿名放行） |
| **xuanchen-ai** | AI 助手与知识库：LangChain4j + Ollama 对话模型，pgvector 向量库 + Apache Tika 文档解析实现知识库 RAG 检索问答 |
| **xuanchen-generator** | 代码生成器：多数据源管理（口令 AES-GCM 加密入库、接口返回掩码）、表/字段 UI 配置、Freemarker 模板一键生成前后端 CRUD 代码（含 `CREATE TABLE IF NOT EXISTS` 建表 DDL） |

### 5.1 核心技术栈（两版共有）

- **MyBatis-Plus**：单表 CRUD、分页插件、逻辑删除（`@TableLogic`）；复杂 SQL 走同名 Mapper XML
- **Druid**：数据源连接池（两版统一）
- **fesod-sheet**：EasyExcel 进入 Apache 孵化后的继任者，支撑各模块 Excel 导入导出（流式读取监听器）
- **MySQL 8 + Redis**：雪花算法字符串主键；Redis 承载会话、登录失败计数、限流（Lua 脚本原子固定窗口）、分片上传块、在线用户等；`RedisUtil.scanKeys` 统一 SCAN，禁用 KEYS
- **WebSocket**：通知公告、审批任务/催办/抄送实时推送（事务提交后再推送）
- **oshi-core**：服务器 CPU、内存、JVM、磁盘指标采集
- **Freemarker**：代码生成器模板引擎
- **java-jwt、Lombok**：无状态令牌与样板代码精简

### 5.2 两套后端的主要差异对照

| 维度 | xuanchen-springboot3 | xuanchen-springboot4 |
|---|---|---|
| 框架 / JDK | Spring Boot 3 / Java 17 | Spring Boot 4 / Java 25 |
| 安全框架 | Apache Shiro（`ShiroFilterFactoryBean` + `JwtFilter`/`ShiroRealm`） | Spring Security（`JwtAuthenticationFilter` + `@PreAuthorize` 方法级鉴权） |
| WebSocket | JSR-356 原生 `@ServerEndpoint` | Spring WebSocket + 握手鉴权 |
| Web starter | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| 业务模块与前端 | 与新版一致，共用同一套 Vue3 前端 | 当前开发与修复主线 |

> 认证令牌约定（两版一致）：前端请求头 `XC-ACCESS-TOKEN` 携带 JWT 访问令牌；Redis 存储会话；刷新令牌经 httpOnly Cookie `XC-REFRESH-TOKEN` 下发。踢人/改密等会话失效场景会同步清理刷新令牌、WebSocket 连接与在线会话，关闭码 1000=正常重连、1008=会话终止。

### 5.3 后端工程实践

- 统一返回结构 `Result` 与 `GlobalExceptionHandler` 全局兜底，业务异常用 `BusinessException` 携带业务码（两版均已具备）
- 多表写操作统一 `@Transactional(rollbackFor = Exception.class)`
- 标志位取值（启用/停用、正常/已删除）统一收敛到 `CommonConst` 常量，禁止裸字面量
- JSON 统一 Jackson 3（`tools.jackson.*`），注解兼容 `com.fasterxml.jackson.annotation.*`
- 敏感配置（数据库口令、MinIO、邮件、AI 模型地址等）一律环境变量注入，仓库内不内置真实口令
- 回收站逻辑：列表数据逻辑删除入站，支持还原（自定义 SQL，规避 `@TableLogic` 过滤）与彻底物理删除（级联清理关系表）

## 六、前端工程 xuanchen-vue3（两套后端通用）

### 6.1 技术栈与工程化

- **Vue 3**（3.5+）：全面采用 `<script setup>` 组合式 API
- **Vite 8**：开发服务器以 `/api` 命名空间代理后端、WebSocket 代理改写 Origin；生产构建同理由 Nginx 反代 `/api`
- **TypeScript**（精确锁版本，规避 vue-tsc 与 TS 7 的兼容问题），`vue-tsc` 全量类型检查纳入构建脚本
- **ant-design-vue 4.x**：UI 组件库；通过 **unplugin-vue-components + AntDesignVueResolver 按需自动引入**
- **Pinia**：组合式 store，管理 auth（会话）、menu（菜单）、tabs（页签）、websocket、collapsed（侧边栏折叠）五块状态
- **vue-router**：静态路由（登录/404）+ **按用户菜单权限动态注入的业务路由**，全局前置守卫统一处理鉴权、动态路由注册与 401 清理；另有全屏静态路由（流程设计器 `/businessprocess/designer/:id`、大屏设计器 `/visualization/designer/:id`、大屏展示 `/visualization/viewer/:code`、报表查看 `/report/viewer/:code`、在线文档编辑 `/fileedit/:id`）
- **axios**：统一实例与拦截器——请求自动带 token；响应统一弹错；blob 下载失败时自动反序列化 JSON 以显示后端真实消息；实例不设默认 `Content-Type`（避免 FormData 被错误序列化）
- **路由约定**：`/api` 是 vite dev proxy / Nginx 的后端转发命名空间，**业务页面路由禁止以 `/api` 开头**；接口服务模块的前端页面路由保留 `/openapi`（对外分发入口为 `/open/api/{apiCode}`），主数据页面为 `/masterdata/*`，在线文档为 `/onlinedoc/*`，文件中心为 `/filecenter/*`，AI 知识库为 `/aiknowledge/*`
- **菜单组件约定**：动态路由用 `import.meta.glob('./views/**/*.vue')` 精确匹配，后台 `sys_menu.component` 必须与 `src/views/` 下**文件名大小写完全一致**（例如 `monitor/onlineuser/OnlineUserList`）。Windows 文件系统不区分大小写，开发时不易暴露，Linux 构建/部署会渲染成"页面组件不存在"占位页；仅修正大小写的文件改名需用 `git mv -f` 才能让 Git 记录该变化
- 其他：ECharts 6（首页看板与大屏/报表图表，卸载时 dispose 防泄漏）、Quill（富文本）、dayjs、bpmn-js 18（流程设计器）、Sass

### 6.2 页面与功能

- **认证页**：登录、注册、忘记密码、滑块验证组件、强制修改密码页
- **系统管理**：账号管理、人员管理（档案 + 部门/岗位分配弹窗）、角色（含分配菜单/用户）、菜单、部门（部门树+部门负责人）、岗位（含分配用户）、字典、参数配置、通知公告
- **系统监控**：服务器监控、缓存监控、在线用户、登录日志、操作日志、接口日志、生成日志、首页数据看板
- **开发工具**：数据源管理、代码生成（表配置、字段 UI 配置）
- **业务中心**：审批流程（定义/设计/申请/待办/已办/我发起的/委托）、文件中心（我的文件/分享/配额/回收站/在线预览）、协同文档（在线 Word/Excel/PPT/绘图/思维导图）、报表中心（数据源/数据集/模板/设计/查看）、可视化大屏（设计/展示）、接口服务（接口/数据/日志）、AI 助手（对话/知识库）
- 每个列表页统一遵循"列表 + 查询表单 + 新增编辑弹窗 + 回收站"的结构

### 6.3 前端工程沉淀

- **hooks**：`useList`（分页查询、增删、批量删除、选中行、查询/重置）、`useStatusToggle`（状态切换乐观更新、失败回滚）、`useDragSort`（拖拽排序）
- **通用业务组件**（`components/xuanchen/`）：
  - `XCListPage` / `XCQueryForm` / `XCModal` / `XCRecycleBin`：标准列表页骨架、查询表单、弹窗、回收站
  - `XCSelect`（字典/远程下拉）、`XCTipIcon`（提示图标）
  - `XCUploadImage` / `XCUploadFile`：图片/文件上传（含水印预览 `XCWatermark`）
  - `XCOfficeViewer`（Office 预览）、`XCCadViewer`（CAD/DXF 预览）、`XCFormRender`（动态表单渲染）
  - `XCAiAssistant`（AI 助手悬浮对话）
- **审批流组件**：`BpmnPreview`（流程图预览）、`BpmApprovalModal`（审批弹窗）、`BpmDelegateModal`（委托弹窗）、`xcModdle`（bpmn-js xc 扩展描述）
- **WebSocket 客户端**：应用层心跳看门狗、半开连接主动断开、指数退避重连（5s 起、封顶 60s）、单连接守卫防重复创建；服务端关闭码 1000 静默重连、1008 停止重连并跳登录
- **多环境构建**：`.env.dev/.env.test/.env.pro`，浏览器侧统一同源相对地址 `/api`，由 Nginx 反代，杜绝写死主机名导致的部署失败

## 七、本地开发环境

### 7.1 后端启动

- 环境：JDK 17+（主线 JDK 25）、Maven 3.9+、MySQL 8、Redis
- 初始化：执行 `Documents/xuanchen-monolithic.sql` 全量导入
- 启动：运行 `xuanchen-boot` 模块主类（默认端口 `SERVER_PORT:8080`，可用环境变量覆盖）
- 编译注意：JDK 23+ 默认 `-proc:none`，命令行编译需加 `-Dmaven.compiler.proc=full`（否则 Lombok 失效）

### 7.2 MinIO 对象存储（文件中心）

文件中心默认使用 MinIO 对象存储（`xuanchen.storage.type=minio`），本地用 Docker 运行：

```bash
docker run -d --name minio \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -v D:/data/minio:/data \
  --restart unless-stopped \
  minio/minio:latest server /data --console-address ":9001"
```

- **9000**：S3 API 端口，后端 `xuanchen.storage.minio.endpoint` 默认 `http://127.0.0.1:9000`，无需改配置
- **9001**：Web 控制台，账号 `minioadmin / minioadmin`
- **桶**：`xc-attach` / `xc-public` 由后端 `MinioStorageService.initBuckets()` 启动时自动创建
- 启停：`docker start minio` / `docker stop minio` / `docker logs -f minio`

> 若暂不使用对象存储，可设 `XUANCHEN_STORAGE_TYPE=local` 回落到本地磁盘（不推荐在正式环境使用）。

### 7.3 可选组件

| 组件 | 用途 | 说明 |
|---|---|---|
| **ONLYOFFICE DocumentServer** | 在线 Office 预览 / 协同编辑 / 在线 Word-Excel-PPT | 配置 `xuanchen.office.ds-url` 与 `xuanchen.office.jwt-secret`（后端与 DS `local.json` 两侧一致） |
| **Ollama + PostgreSQL(pgvector)** | AI 助手对话 / 知识库 RAG | LangChain4j 对接，模型与向量库地址经 yml/环境变量配置 |
| **LibreOffice / CAD 转换工具** | 文档格式转换、CAD 预览 | 路径经配置项注入，缺省不影响其他功能 |

## 八、系统功能截图

> 以下截图均来自**实际运行的系统**（Spring Boot 4 + Vue 3），分辨率 1600×900。点击图片可查看原图；完整截图共 **53 张**，存放于 [`Documents/系统截图/`](Documents/系统截图/)，下面按功能分组展示其中 **24 张**——设计器、编辑器等核心能力页优先，列表页仅保留代表性页面。

### 8.1 登录与工作台

| | |
|---|---|
| [![登录页](Documents/系统截图/01-登录页.png)](Documents/系统截图/01-登录页.png)<br>**登录页**：账号密码 + 滑块验证码 + 记住我 / 忘记密码 / 注册 | [![首页工作台](Documents/系统截图/02-首页看板.png)](Documents/系统截图/02-首页看板.png)<br>**首页工作台**：未读公告、我的待办、今日登录、快捷入口与多页签工作区 |

### 8.2 基础数据与系统管理

| | |
|---|---|
| [![人员管理](Documents/系统截图/03-人员管理.png)](Documents/系统截图/03-人员管理.png)<br>**人员管理**：人员档案与部门/岗位分配 | [![部门管理](Documents/系统截图/04-部门管理.png)](Documents/系统截图/04-部门管理.png)<br>**部门管理**：树形组织与部门负责人 |
| [![账号管理](Documents/系统截图/06-账号管理.png)](Documents/系统截图/06-账号管理.png)<br>**账号管理**：登录凭证、角色分配、回收站 | [![菜单管理](Documents/系统截图/08-菜单管理.png)](Documents/系统截图/08-菜单管理.png)<br>**菜单管理**：菜单树驱动前端动态路由 |

### 8.3 系统监控与日志

| | |
|---|---|
| [![在线用户](Documents/系统截图/12-在线用户.png)](Documents/系统截图/12-在线用户.png)<br>**在线用户**：会话查看与强制下线 | [![性能监控](Documents/系统截图/13-性能监控.png)](Documents/系统截图/13-性能监控.png)<br>**性能监控**：CPU / 内存 / JVM / 磁盘 |
| [![缓存监控](Documents/系统截图/14-缓存监控.png)](Documents/系统截图/14-缓存监控.png)<br>**缓存监控**：Redis 信息与 Key 浏览 | [![登录日志](Documents/系统截图/15-登录日志.png)](Documents/系统截图/15-登录日志.png)<br>**登录日志**：登录账号、时间、IP、终端与结果 |

### 8.4 文件中心与在线协同文档

| | |
|---|---|
| [![我的文件](Documents/系统截图/19-我的文件.png)](Documents/系统截图/19-我的文件.png)<br>**我的文件**：文件夹、上传/秒传、预览、协同编辑、分享 | [![流程图编辑](Documents/系统截图/44-流程图编辑.png)](Documents/系统截图/44-流程图编辑.png)<br>**流程图编辑**：拖拽图形、连线、导出与保存 |
| [![协同文档编辑](Documents/系统截图/46-协同文档编辑.png)](Documents/系统截图/46-协同文档编辑.png)<br>**协同文档编辑**：ONLYOFFICE 在线 Word / Excel / PPT 协同编辑 | [![思维导图编辑](Documents/系统截图/45-思维导图编辑.png)](Documents/系统截图/45-思维导图编辑.png)<br>**思维导图编辑**：节点树编辑、缩放定位与导出 |

### 8.5 流程审批（自研 BPM 引擎）

| | |
|---|---|
| [![在线表单设计](Documents/系统截图/47-在线表单设计.png)](Documents/系统截图/47-在线表单设计.png)<br>**在线表单设计**：控件拖拽、字段属性、预览与保存 | [![流程设计](Documents/系统截图/48-流程设计.png)](Documents/系统截图/48-流程设计.png)<br>**流程设计**：bpmn-js 建模、审批节点/网关、绑定在线表单 |

### 8.6 报表 / 接口服务 / 数据大屏

| | |
|---|---|
| [![报表模板设计](Documents/系统截图/49-报表模板设计.png)](Documents/系统截图/49-报表模板设计.png)<br>**报表模板设计**：表格画布 + 数据集 + 单元格字段绑定 | [![接口编辑](Documents/系统截图/50-接口编辑.png)](Documents/系统截图/50-接口编辑.png)<br>**接口编辑**：接口编码/密钥、步骤链编排、测试运行与发布 |
| [![数据大屏设计](Documents/系统截图/51-大屏设计.png)](Documents/系统截图/51-大屏设计.png)<br>**数据大屏设计**：组件拖拽、图表指标、分辨率与背景设置 | [![数据大屏](Documents/系统截图/35-数据大屏.png)](Documents/系统截图/35-数据大屏.png)<br>**数据大屏**：大屏卡片与全屏设计入口 |

### 8.7 代码生成器 / AI 知识库 / 通知待办

| | |
|---|---|
| [![代码生成新增](Documents/系统截图/52-代码生成新增.png)](Documents/系统截图/52-代码生成新增.png)<br>**代码生成新增**：项目预设、数据源、表/字段配置、输出目录、一键生成 | [![知识库文档](Documents/系统截图/53-知识库文档.png)](Documents/系统截图/53-知识库文档.png)<br>**知识库文档**：文档管理抽屉（上传/录入/导入、解析状态与分块数） |
| [![待办事项](Documents/系统截图/36-待办事项.png)](Documents/系统截图/36-待办事项.png)<br>**待办事项**：审批任务统一待办 | [![发布通知](Documents/系统截图/37-发布通知.png)](Documents/系统截图/37-发布通知.png)<br>**发布通知**：通知公告编辑与发布 |

> 其余截图（各功能列表页、回收站、项目预设、检索测试、各类日志、文件配额、通知管理/列表等）同样保存在 `Documents/系统截图/`。

## 九、系统特点小结

1. **双版本可对照演进**：同一业务模型分别在 Shiro/SpringBoot3 与 Spring Security/SpringBoot4 上落地，模块划分与接口契约一致，既是可运行系统，也是两套安全体系迁移升级的对照样本。
2. **业务模块开箱即用**：自研审批流引擎（bpmn-js 可视化建模）、文件中心（秒传/分片/分享/配额/回收站）、在线协同 Office 文档、报表中心、可视化大屏、接口服务编排与开放分发、AI 知识库 RAG——覆盖中后台常见业务场景。
3. **权限闭环完整**：菜单级动态路由 + 接口级鉴权 + 角色菜单多对多授权，配登录失败锁定（用户名 + IP 双维度）、滑块验证码、密码复杂度策略形成多层安全防护。
4. **账号与人员解耦**：登录凭证（`sys_account`）与人事档案（`md_person` 主数据）分离建模，1 人员可关联 N 账号，职责边界清晰。
5. **工程规范统一**：前后端均有明确的分层约定与可复用抽象（Result/全局异常、useList/通用组件、常量收敛、类型声明完备），两套后端严格同构，改一处必同步另一处。
6. **运维可观测**：操作/登录/接口/生成日志、在线用户、服务器与 Redis 监控齐备，通知与审批任务支持 WebSocket 实时触达。
7. **可扩展的代码生成**：数据源加密托管 + 模板化生成，新增业务模块可低成本产出标准 CRUD（含建表 DDL）。
8. **技术栈新**：新版采用 Spring Boot 4 + Java 25、Vite 8、Vue 3、ant-design-vue 4，适合作为现代 Java/Vue 技术栈的学习与二开底座；旧版则为 JDK 17 保守环境提供兼容选择。
