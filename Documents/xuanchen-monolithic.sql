/*
 Navicat Premium Dump SQL

 Source Server         : 本地-MySQL
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : xuanchen-monolithic

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 06/11/2026 04:24:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_document
-- ----------------------------
DROP TABLE IF EXISTS `ai_document`;
CREATE TABLE `ai_document`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `kb_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属知识库id',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件名',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MinIO对象id（复用文件中心存储）',
  `file_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型(pdf/docx/xlsx/txt/md...)',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容指纹(32位小写MD5,同知识库内容查重用)',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '解析状态(pending待解析/parsing解析中/ready就绪/failed失败)',
  `chunk_count` int NULL DEFAULT 0 COMMENT '分块数量',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '解析失败原因',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标志(0正常1删除)',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_doc_kb`(`kb_id` ASC) USING BTREE,
  INDEX `idx_ai_doc_kb_md5`(`kb_id` ASC, `md5` ASC) USING BTREE,
  INDEX `idx_ai_doc_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI知识库文档' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_document
-- ----------------------------

-- ----------------------------
-- Table structure for ai_kb
-- ----------------------------
DROP TABLE IF EXISTS `ai_kb`;
CREATE TABLE `ai_kb`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `kb_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `kb_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `embed_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'bge-m3' COMMENT '向量化模型（入库时的模型，更换模型需重新向量化）',
  `chunk_size` int NULL DEFAULT 500 COMMENT '分块大小（字符）',
  `chunk_overlap` int NULL DEFAULT 50 COMMENT '分块重叠（字符）',
  `status` int NULL DEFAULT 1 COMMENT '状态(1启用0停用)',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标志(0正常1删除)',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_kb_name`(`kb_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI知识库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_kb
-- ----------------------------
INSERT INTO `ai_kb` VALUES ('2116899184253333506', '个人知识库', NULL, 'bge-m3', 500, 50, 1, 0, 'admin4', '2026-11-01 22:24:30', 'admin4', '2026-11-01 22:24:30');
INSERT INTO `ai_kb` VALUES ('2116899211285622786', '公司知识库', NULL, 'bge-m3', 500, 50, 1, 0, 'admin4', '2026-11-01 22:24:37', 'admin4', '2026-11-01 22:24:37');

-- ----------------------------
-- Table structure for ai_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `session_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属会话id',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色(user用户/assistant助手)',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容（Markdown）',
  `refs` json NULL COMMENT '引用列表（命中的文档名/分块摘要/分块id）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_msg_session`(`session_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI问答消息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_message
-- ----------------------------

-- ----------------------------
-- Table structure for ai_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_session`;
CREATE TABLE `ai_session`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属用户（关联 sys_user_account.user_name）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题（默认取首条提问截断）',
  `kb_ids` json NULL COMMENT '会话绑定的知识库id列表',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_session_user`(`user_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI问答会话' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_session
-- ----------------------------

-- ----------------------------
-- Table structure for api
-- ----------------------------
DROP TABLE IF EXISTS `api`;
CREATE TABLE `api`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '雪花ID',
  `api_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接口编码（访问路径 /open/api/{api_code}）',
  `api_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接口名称',
  `api_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问密钥（空=免鉴权，非空则请求须带 X-Api-Key 头或 apiKey 参数）',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态（0草稿，1已发布，2已停用）',
  `steps` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '步骤链配置JSON（数组：sql/http/map/save 四类步骤）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_api_code`(`api_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '接口 接口定义' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of api
-- ----------------------------
INSERT INTO `api` VALUES ('2116897640980148225', 'test', '测试', NULL, 0, '[{\"type\":\"save\",\"name\":\"archive\",\"config\":{\"source\":\"params\"}}]', '', 0, 'admin4', '2026-11-01 22:18:22', 'admin4', '2026-11-01 22:18:22');

-- ----------------------------
-- Table structure for api_data
-- ----------------------------
DROP TABLE IF EXISTS `api_data`;
CREATE TABLE `api_data`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '雪花ID',
  `api_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源接口编码',
  `data` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '采集数据JSON',
  `create_time` datetime NULL DEFAULT NULL COMMENT '采集时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_open_data_code_time`(`api_code` ASC, `create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '接口 采集数据' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of api_data
-- ----------------------------

-- ----------------------------
-- Table structure for api_schedule
-- ----------------------------
DROP TABLE IF EXISTS `api_schedule`;
CREATE TABLE `api_schedule`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '雪花ID',
  `api_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接口编码',
  `cron` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron 表达式（Spring 6 位格式）',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0停用，1启用）',
  `last_run_time` datetime NULL DEFAULT NULL COMMENT '上次执行时间',
  `next_run_time` datetime NULL DEFAULT NULL COMMENT '下次执行时间（到点即被调度器扫描执行）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_open_sched_next`(`enabled` ASC, `next_run_time` ASC) USING BTREE,
  INDEX `idx_open_sched_code`(`api_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '接口 定时采集配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of api_schedule
-- ----------------------------

-- ----------------------------
-- Table structure for bpm_definition
-- ----------------------------
DROP TABLE IF EXISTS `bpm_definition`;
CREATE TABLE `bpm_definition`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程编码（唯一）',
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型（业务模块接入标识）',
  `version` int NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态（0草稿 1启用 2停用）',
  `bpmn_xml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'BPMN 2.0 XML（设计器回显/渲染高亮）',
  `online_form_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定的在线表单ID（可空=不使用在线表单）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_def_code`(`code` ASC) USING BTREE,
  INDEX `idx_bpm_def_business`(`business_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批流程定义' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_definition
-- ----------------------------
INSERT INTO `bpm_definition` VALUES ('2116894356189999106', '流程审批-测试001', 'LCSP_TEST001', 'common', 1, 1, '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xc=\"http://xuanchen.workbuddy.cn/schema/bpmn/xc\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"xc-definitions\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"xc-process\" isExecutable=\"false\">\n    <bpmn:startEvent id=\"StartEvent_1\" name=\"开始\">\n      <bpmn:outgoing>Flow_1nyf6hn</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:userTask id=\"Activity_1ltn8xy\" xc:rules=\"[{&#34;ruleType&#34;:1,&#34;ruleValue&#34;:&#34;admin4&#34;}]\">\n      <bpmn:incoming>Flow_1nyf6hn</bpmn:incoming>\n      <bpmn:outgoing>Flow_00cs2gv</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:endEvent id=\"Event_1xv0kf8\">\n      <bpmn:incoming>Flow_00cs2gv</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_1nyf6hn\" sourceRef=\"StartEvent_1\" targetRef=\"Activity_1ltn8xy\" />\n    <bpmn:sequenceFlow id=\"Flow_00cs2gv\" sourceRef=\"Activity_1ltn8xy\" targetRef=\"Event_1xv0kf8\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"xc-diagram\">\n    <bpmndi:BPMNPlane id=\"xc-plane\" bpmnElement=\"xc-process\">\n      <bpmndi:BPMNShape id=\"StartEvent_1_di\" bpmnElement=\"StartEvent_1\">\n        <dc:Bounds x=\"-188\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"-181\" y=\"178\" width=\"22\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1ltn8xy_di\" bpmnElement=\"Activity_1ltn8xy\">\n        <dc:Bounds x=\"-50\" y=\"120\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Event_1xv0kf8_di\" bpmnElement=\"Event_1xv0kf8\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1nyf6hn_di\" bpmnElement=\"Flow_1nyf6hn\">\n        <di:waypoint x=\"-152\" y=\"160\" />\n        <di:waypoint x=\"-50\" y=\"160\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_00cs2gv_di\" bpmnElement=\"Flow_00cs2gv\">\n        <di:waypoint x=\"50\" y=\"160\" />\n        <di:waypoint x=\"142\" y=\"160\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n', '2116893774049964034', '', 0, 'admin4', '2026-11-01 22:05:19', 'admin4', '2026-11-01 22:07:53');

-- ----------------------------
-- Table structure for bpm_delegate
-- ----------------------------
DROP TABLE IF EXISTS `bpm_delegate`;
CREATE TABLE `bpm_delegate`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '委托人（登录名）',
  `delegate_user_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '被委托人（登录名）',
  `start_time` datetime NULL DEFAULT NULL COMMENT '生效开始时间（空=立即）',
  `end_time` datetime NULL DEFAULT NULL COMMENT '生效结束时间（空=长期）',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注（如请假事由）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_delegate_user`(`user_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批委托/代理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_delegate
-- ----------------------------

-- ----------------------------
-- Table structure for bpm_flow
-- ----------------------------
DROP TABLE IF EXISTS `bpm_flow`;
CREATE TABLE `bpm_flow`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `definition_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程定义ID',
  `flow_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '连线标识（BPMN id）',
  `from_node_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '源节点标识',
  `to_node_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标节点标识',
  `flow_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '连线名称',
  `condition_expr` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '条件表达式（如 amount > 5000）',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否默认流（网关兜底，1是 0否）',
  `sort_no` int NULL DEFAULT NULL COMMENT '排序码',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_flow_def`(`definition_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批流程连线' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_flow
-- ----------------------------
INSERT INTO `bpm_flow` VALUES ('2116895000338624514', '2116894356189999106', 'Flow_1nyf6hn', 'StartEvent_1', 'Activity_1ltn8xy', '', NULL, 0, 0);
INSERT INTO `bpm_flow` VALUES ('2116895000338624515', '2116894356189999106', 'Flow_00cs2gv', 'Activity_1ltn8xy', 'Event_1xv0kf8', '', NULL, 0, 1);

-- ----------------------------
-- Table structure for bpm_instance
-- ----------------------------
DROP TABLE IF EXISTS `bpm_instance`;
CREATE TABLE `bpm_instance`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `definition_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程定义ID',
  `definition_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流程编码',
  `definition_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流程名称',
  `version` int NOT NULL DEFAULT 1 COMMENT '发起时的定义版本（展示用）',
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务单据ID',
  `business_summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务摘要（列表展示/待办标题）',
  `apply_user` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发起人（sys_account.user_name）',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（1审批中 2通过 3驳回 4撤回）',
  `current_node_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前节点标识（多个待处理时为最近推进节点）',
  `form_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '业务表单快照 JSON（条件表达式上下文）',
  `graph_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图模型快照 JSON（nodes/flows/rules，发起时冻结，在途实例不受定义修改影响）',
  `token_state` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '并行网关token状态JSON（active存活数+arrivals汇聚计数，办结后清空）',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '办结时间',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间（=发起时间）',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_inst_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_bpm_inst_apply`(`apply_user` ASC) USING BTREE,
  INDEX `idx_bpm_inst_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批流程实例' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_instance
-- ----------------------------
INSERT INTO `bpm_instance` VALUES ('2116895220539584513', '2116894356189999106', 'LCSP_TEST001', '流程审批-测试001', 1, 'common', 'apply_1793542125178', '流程审批-测试001-申请摘要', 'admin4', 2, 'Activity_1ltn8xy', '{\"input1\":\"AA\",\"input2\":\"BB\",\"input3\":\"CC\",\"input4\":\"DD\",\"textarea5\":\"EE\",\"number6\":66.66,\"date7\":\"2026-11-11\",\"select8\":\"选项1\",\"radio9\":\"选项2\",\"checkbox10\":[\"选项1\",\"选项2\",\"选项3\"],\"switch11\":true}', '{\"flows\":[{\"condition\":null,\"defaultFlow\":false,\"from\":\"StartEvent_1\",\"key\":\"Flow_1nyf6hn\",\"name\":\"\",\"to\":\"Activity_1ltn8xy\"},{\"condition\":null,\"defaultFlow\":false,\"from\":\"Activity_1ltn8xy\",\"key\":\"Flow_00cs2gv\",\"name\":\"\",\"to\":\"Event_1xv0kf8\"}],\"nodes\":[{\"approvalMode\":null,\"key\":\"StartEvent_1\",\"name\":\"开始\",\"type\":2},{\"approvalMode\":1,\"key\":\"Activity_1ltn8xy\",\"name\":\"\",\"type\":1},{\"approvalMode\":null,\"key\":\"Event_1xv0kf8\",\"name\":\"结束\",\"type\":3}],\"rules\":[{\"nodeKey\":\"Activity_1ltn8xy\",\"ruleType\":1,\"ruleValue\":\"admin4\"}]}', NULL, '2026-11-01 22:09:18', 0, 'admin4', '2026-11-01 22:08:45', 'admin4', '2026-11-01 22:09:18');

-- ----------------------------
-- Table structure for bpm_node
-- ----------------------------
DROP TABLE IF EXISTS `bpm_node`;
CREATE TABLE `bpm_node`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `definition_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点标识（BPMN id）',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节点名称',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（1用户任务 2开始 3结束 4排他网关）',
  `approval_mode` tinyint(1) NULL DEFAULT NULL COMMENT '审批方式（1或签 2会签，仅用户任务）',
  `sort_no` int NULL DEFAULT NULL COMMENT '排序码',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_node_def`(`definition_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批流程节点' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_node
-- ----------------------------
INSERT INTO `bpm_node` VALUES ('2116895000279904257', '2116894356189999106', 'StartEvent_1', '开始', 2, NULL, 0);
INSERT INTO `bpm_node` VALUES ('2116895000309264386', '2116894356189999106', 'Activity_1ltn8xy', '', 1, 1, 1);
INSERT INTO `bpm_node` VALUES ('2116895000309264387', '2116894356189999106', 'Event_1xv0kf8', '结束', 3, NULL, 2);

-- ----------------------------
-- Table structure for bpm_node_rule
-- ----------------------------
DROP TABLE IF EXISTS `bpm_node_rule`;
CREATE TABLE `bpm_node_rule`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `definition_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点标识',
  `rule_type` tinyint(1) NOT NULL COMMENT '规则类型（1指定用户 2指定角色 3发起人自选）',
  `rule_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规则值（用户名逗号分隔/角色编码）',
  `sort_no` int NULL DEFAULT NULL COMMENT '排序码',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_rule_def`(`definition_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '节点审批人规则' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_node_rule
-- ----------------------------
INSERT INTO `bpm_node_rule` VALUES ('2116895000367984641', '2116894356189999106', 'Activity_1ltn8xy', 1, 'admin4', 0);

-- ----------------------------
-- Table structure for bpm_record
-- ----------------------------
DROP TABLE IF EXISTS `bpm_record`;
CREATE TABLE `bpm_record`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `instance_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程实例ID',
  `node_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节点标识',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节点名称',
  `operator` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人（sys_account.user_name）',
  `action` tinyint(1) NOT NULL COMMENT '动作（1提交 2同意 3驳回 4撤回）',
  `opinion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '意见',
  `create_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_record_inst`(`instance_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_record
-- ----------------------------
INSERT INTO `bpm_record` VALUES ('2116895220564750338', '2116895220539584513', NULL, NULL, 'admin4', 1, NULL, '2026-11-01 22:08:45');
INSERT INTO `bpm_record` VALUES ('2116895355667476481', '2116895220539584513', 'Activity_1ltn8xy', '', 'admin4', 2, '同意', '2026-11-01 22:09:17');

-- ----------------------------
-- Table structure for bpm_task
-- ----------------------------
DROP TABLE IF EXISTS `bpm_task`;
CREATE TABLE `bpm_task`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `instance_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程实例ID',
  `node_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点标识',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节点名称',
  `assignee` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '审批人（sys_account.user_name）',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（1待处理 2已处理 3已作废）',
  `result` tinyint(1) NULL DEFAULT NULL COMMENT '处理结果（1同意 2驳回）',
  `opinion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批意见',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间（=任务生成时间）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bpm_task_inst`(`instance_id` ASC) USING BTREE,
  INDEX `idx_bpm_task_assignee`(`assignee` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批任务' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bpm_task
-- ----------------------------
INSERT INTO `bpm_task` VALUES ('2116895220799631362', '2116895220539584513', 'Activity_1ltn8xy', '', 'admin4', 2, 1, '同意', '2026-11-01 22:09:17', '2026-11-01 22:08:45');

-- ----------------------------
-- Table structure for file_collaborator
-- ----------------------------
DROP TABLE IF EXISTS `file_collaborator`;
CREATE TABLE `file_collaborator`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键（雪花）',
  `file_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件id（file_info.id）',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协作者登录名',
  `permission` tinyint(1) NOT NULL DEFAULT 1 COMMENT '协作权限（1=可编辑，2=只读）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者（文件属主）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_collab`(`file_id` ASC, `username` ASC) USING BTREE,
  INDEX `idx_file_collab_user`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件协作成员' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_collaborator
-- ----------------------------

-- ----------------------------
-- Table structure for file_folder
-- ----------------------------
DROP TABLE IF EXISTS `file_folder`;
CREATE TABLE `file_folder`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键（雪花）',
  `folder_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件夹名',
  `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '父文件夹id（0=根目录）',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '归属人登录名',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_folder_user`(`username` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件中心文件夹' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_folder
-- ----------------------------
INSERT INTO `file_folder` VALUES ('2116891628806074370', '萱晨', '0', 'admin4', 0, 'admin4', '2026-11-01 21:54:29', 'admin4', '2026-11-01 21:54:29');
INSERT INTO `file_folder` VALUES ('2116891772804919298', '烈鹰', '0', 'admin4', 0, 'admin4', '2026-11-01 21:55:03', 'admin4', '2026-11-01 21:55:03');

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键（雪花）',
  `bucket` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储桶名',
  `object_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储键（bizType/yyyyMM/雪花_原名.ext）',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名',
  `ext` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '扩展名（小写，jpeg 归一为 jpg）',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MIME 类型',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `storage_type` tinyint(1) NULL DEFAULT 1 COMMENT '存储类型（0本地 1MinIO）',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'common' COMMENT '业务类型（avatar/photo/common/convert...）',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上传人登录名',
  `md5` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容MD5（二期秒传去重用）',
  `folder_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件夹id（file_folder.id，空=根目录）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除=回收站）',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间（进回收站时记录，超期由定时任务彻底清理）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_info_user`(`username` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_file_info_biz`(`biz_type` ASC) USING BTREE,
  INDEX `idx_file_info_md5`(`md5` ASC) USING BTREE,
  INDEX `idx_file_info_recycle`(`username` ASC, `delete_time` ASC) USING BTREE,
  INDEX `idx_file_info_folder`(`username` ASC, `folder_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件元数据' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_info
-- ----------------------------
INSERT INTO `file_info` VALUES ('2116893110360076290', 'xc-attach', 'online/202611/系统文档-word-测试001_20261101220022076_19a80.docx', '协同文档-word-测试001.docx', 'docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 25458, 1, 'online', 'admin4', 'c18328e40720f1891f831fe00bb4d81b', NULL, 0, NULL, 'admin4', '2026-11-01 22:00:22', 'anonymousUser', '2026-11-01 22:01:30');
INSERT INTO `file_info` VALUES ('2116893453626109954', 'xc-attach', 'online/202611/协同文档-excel-测试001_20261101220143965_1772b.xlsx', '协同文档-excel-测试001.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 7902, 1, 'online', 'admin4', '8baf19de1f232038bbbdaa1ecb06f8b4', NULL, 0, NULL, 'admin4', '2026-11-01 22:01:44', 'anonymousUser', '2026-11-01 22:02:06');
INSERT INTO `file_info` VALUES ('2116893597830475777', 'xc-attach', 'online/202611/协同文档-ppt-测试001_20261101220218284_1074e.pptx', '协同文档-ppt-测试001.pptx', 'pptx', 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 36850, 1, 'online', 'admin4', '871dc11dfaceb42c0bd0ddc245ff3282', NULL, 0, NULL, 'admin4', '2026-11-01 22:02:18', 'anonymousUser', '2026-11-01 22:02:49');

-- ----------------------------
-- Table structure for file_quota
-- ----------------------------
DROP TABLE IF EXISTS `file_quota`;
CREATE TABLE `file_quota`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键（雪花）',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录名',
  `quota_bytes` bigint NOT NULL COMMENT '配额上限（字节，0=不限制）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_quota_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件中心用户配额' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_quota
-- ----------------------------

-- ----------------------------
-- Table structure for file_share
-- ----------------------------
DROP TABLE IF EXISTS `file_share`;
CREATE TABLE `file_share`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键（雪花）',
  `share_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分享编号（公开链接使用，随机UUID）',
  `file_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件id（file_info.id）',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分享人登录名',
  `share_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提取码（空=无需提取码）',
  `expire_time` datetime NOT NULL COMMENT '失效时间',
  `visit_count` int NULL DEFAULT 0 COMMENT '下载次数',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '撤销状态（0有效，1已撤销）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_share_no`(`share_no` ASC) USING BTREE,
  INDEX `idx_file_share_user`(`username` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_file_share_file`(`file_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件共享' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_share
-- ----------------------------

-- ----------------------------
-- Table structure for gen_project_profile
-- ----------------------------
DROP TABLE IF EXISTS `gen_project_profile`;
CREATE TABLE `gen_project_profile`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `profile_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预设名称',
  `database_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认统一数据源ID（sys_datasource.id）',
  `output_dir` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '输出目录',
  `backend_rel_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '后端相对路径',
  `frontend_rel_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '前端相对路径',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '包名',
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模块名',
  `parent_menu_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '上级菜单ID',
  `template` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'crud' COMMENT '生成模板（crud/backend）',
  `order_no` int NULL DEFAULT 0 COMMENT '排序码',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成 项目预设' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_project_profile
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `database_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '统一数据源ID（sys_datasource.id）',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表名称',
  `if_create_table` tinyint(1) NULL DEFAULT 0 COMMENT '是否创建表（1是：目标库无此表，生成前按字段配置建表；0否）',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表注释',
  `sub_table_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
  `parent_menu_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '上级菜单ID（menu.sql 挂载位置，0为根）',
  `sub_table_fk_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
  `template` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud 完整 / backend 仅后端）',
  `output_dir` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '/' COMMENT '输出目录',
  `backend_rel_path` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '后端输出相对路径（相对输出目录，空=直接输出到根）',
  `frontend_rel_path` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '前端输出相对路径（相对输出目录，空=根/vue）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '包名',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模块名',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成 表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `table_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表ID',
  `column_scope` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'main' COMMENT '字段归属：main主表/sub子表',
  `sub_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属子表配置ID（NULL=主表字段）',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '字段 名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '字段 注释',
  `column_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '字段 类型',
  `column_length` int NULL DEFAULT NULL COMMENT '字段 长度',
  `column_scale` int NULL DEFAULT NULL COMMENT '字段 小数点',
  `column_default` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '字段 默认值',
  `column_is_pk` tinyint(1) NULL DEFAULT NULL COMMENT '字段 是否主键',
  `column_is_nullable` tinyint(1) NULL DEFAULT NULL COMMENT '字段 是否允许为NULL',
  `ui_is_query` tinyint(1) NULL DEFAULT NULL COMMENT '界面 是否查询字段',
  `ui_query_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '界面 查询方式',
  `ui_is_edit` tinyint(1) NULL DEFAULT NULL COMMENT '界面 是否编辑字段',
  `ui_is_required` tinyint(1) NULL DEFAULT NULL COMMENT '界面 是否必填字段',
  `ui_edit_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '界面 编辑方式',
  `ui_dict_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '界面 字典编码（选择类控件）',
  `ui_is_list` tinyint(1) NULL DEFAULT NULL COMMENT '界面 是否列表字段',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成 列' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table_sub
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_sub`;
CREATE TABLE `gen_table_sub`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `table_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主表配置ID（gen_table.id）',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子表物理表名',
  `sub_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子表关联主表的外键列',
  `sub_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子表标题（留空取物理表注释）',
  `order_no` int NULL DEFAULT 0 COMMENT '排序码（决定页面里子表的展示顺序）',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_id`(`table_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成 子表配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table_sub
-- ----------------------------

-- ----------------------------
-- Table structure for log_api
-- ----------------------------
DROP TABLE IF EXISTS `log_api`;
CREATE TABLE `log_api`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '雪花ID',
  `api_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接口编码',
  `trigger_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '触发方式（1手动调用，2定时采集，3测试运行）',
  `success` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否成功（0失败，1成功）',
  `cost_ms` int NULL DEFAULT NULL COMMENT '耗时（毫秒）',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NULL DEFAULT NULL COMMENT '调用时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_open_log_code_time`(`api_code` ASC, `create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日志 接口' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of log_api
-- ----------------------------

-- ----------------------------
-- Table structure for log_generator
-- ----------------------------
DROP TABLE IF EXISTS `log_generator`;
CREATE TABLE `log_generator`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `table_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表配置ID',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物理表名',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表注释',
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模块名',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '包名',
  `generate_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成方式（DISK落盘/ZIP下载/PREVIEW预览）',
  `template` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用的模板',
  `file_count` int NULL DEFAULT 0 COMMENT '文件数',
  `file_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文件清单（换行分隔）',
  `success` tinyint(1) NULL DEFAULT 1 COMMENT '是否成功（1成功，0失败）',
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结果说明/失败原因',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '生成时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_id`(`table_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日志 代码生成' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of log_generator
-- ----------------------------

-- ----------------------------
-- Table structure for log_login
-- ----------------------------
DROP TABLE IF EXISTS `log_login`;
CREATE TABLE `log_login`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `login_time` datetime NULL DEFAULT NULL COMMENT '登录时间',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录IP地址',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '登录状态(0失败1成功)',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '描述信息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日志 登录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of log_login
-- ----------------------------
INSERT INTO `log_login` VALUES ('2108835700995158018', 'admin4', '2026-10-10 16:23:06', '127.0.0.1', 0, '用户名或密码错误');
INSERT INTO `log_login` VALUES ('2108835733970776066', 'admin4', '2026-10-10 16:23:14', '127.0.0.1', 1, '登录成功');

-- ----------------------------
-- Table structure for log_operation
-- ----------------------------
DROP TABLE IF EXISTS `log_operation`;
CREATE TABLE `log_operation`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作用户',
  `module` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模块',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `operation_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `operation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作类型（如：新增、修改、删除等）',
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '操作状态(0失败1成功)',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '操作描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日志 操作' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of log_operation
-- ----------------------------
INSERT INTO `log_operation` VALUES ('2108833365971578881', 'admin4', 'system', '127.0.0.1', '2026-10-10 16:13:49', '列表', '/system/config/list', 'configName=&configDescription=& body=[{\"configDescription\":\"\",\"configKey\":null,\"configName\":\"\",\"configType\":null,\"configValue\":null,\"createBy\":null,\"createTime\":null,\"delFlag\":null,\"id\":null,\"orderNo\":null,\"status\":null,\"updateBy\":null,\"updateTime\":null},1,10]', 1, '操作成功!');
INSERT INTO `log_operation` VALUES ('2108834599067594754', 'admin4', 'system', '127.0.0.1', '2026-10-10 16:18:43', '列表', '/system/config/list', 'configName=&configDescription=& body=[{\"configDescription\":\"\",\"configKey\":null,\"configName\":\"\",\"configType\":null,\"configValue\":null,\"createBy\":null,\"createTime\":null,\"delFlag\":null,\"id\":null,\"orderNo\":null,\"status\":null,\"updateBy\":null,\"updateTime\":null},1,10]', 1, '操作成功!');

-- ----------------------------
-- Table structure for md_dept
-- ----------------------------
DROP TABLE IF EXISTS `md_dept`;
CREATE TABLE `md_dept`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门ID',
  `dept_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门编码',
  `parent_dept_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父部门编码',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '部门名称',
  `is_leaf` tinyint(1) NULL DEFAULT NULL COMMENT '是否叶子节点（1是，0不是）',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `leader_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门负责人（sys_account.user_name 登录名，可空）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主数据 部门' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_dept
-- ----------------------------
INSERT INTO `md_dept` VALUES ('1995995435964747777', 'XC001', NULL, '萱晨集团', NULL, 1, 1, 0, 'admin', '2025-12-03 07:16:10', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995478822146050', 'XC001001', 'XC001', '萱晨软件', NULL, 1, 1, 0, 'admin', '2025-12-03 07:16:21', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995543003385857', 'XC001002', 'XC001', '萱晨硬件', NULL, 2, 1, 0, 'admin', '2025-12-03 07:16:36', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995637148733441', 'XC001001001', 'XC001001', '销售部', NULL, 1, 1, 0, 'admin', '2025-12-03 07:16:58', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995682933755905', 'XC001001002', 'XC001001', '工程部', NULL, 2, 1, 0, 'admin', '2025-12-03 07:17:09', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995738382454786', 'XC001001003', 'XC001001', '技术部', NULL, NULL, 1, 0, 'admin', '2025-12-03 07:17:23', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995777896992769', 'XC001001004', 'XC001001', '研发部', NULL, 4, 1, 0, 'admin', '2025-12-03 07:17:32', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995817906458625', 'XC001001005', 'XC001001', '宣传部', NULL, NULL, 1, 0, 'admin', '2025-12-03 07:17:41', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995900093845506', 'XC001002001', 'XC001002', '销售部', NULL, 1, 1, 0, 'admin', '2025-12-03 07:18:01', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995938803077122', 'XC001002002', 'XC001002', '工程部', NULL, 2, 1, 0, 'admin', '2025-12-03 07:18:10', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995995976459538433', 'XC001002003', 'XC001002', '技术部', NULL, 3, 1, 0, 'admin', '2025-12-03 07:18:19', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995996035045576705', 'XC001002004', 'XC001002', '研发部', NULL, 4, 1, 0, 'admin', '2025-12-03 07:18:33', NULL, NULL, NULL);
INSERT INTO `md_dept` VALUES ('1995996075965206530', 'XC001002005', 'XC001002', '宣传部', NULL, 5, 1, 0, 'admin', '2025-12-03 07:18:43', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for md_person
-- ----------------------------
DROP TABLE IF EXISTS `md_person`;
CREATE TABLE `md_person`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `id_card` char(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `nation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '民族',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '户籍 / 常住地址',
  `native_place` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '籍贯',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别：1男2女',
  `marital_status` tinyint NULL DEFAULT NULL COMMENT '婚姻状况：1未婚2已婚3离异4丧偶',
  `education` tinyint NULL DEFAULT NULL COMMENT '学历:1小学2初中3高中4大专5本科6研究生',
  `person_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人员编码',
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人员照片',
  `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号（档案联系方式）',
  `emergency_contact` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱（档案联系方式）',
  `entry_date` date NULL DEFAULT NULL COMMENT '入职日期',
  `leave_date` date NULL DEFAULT NULL COMMENT '离职日期',
  `person_type` tinyint NULL DEFAULT NULL COMMENT '人员类型：1正式员工2外协3临时人员',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1在职2试用3离职4退休）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主数据 人员' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_person
-- ----------------------------
INSERT INTO `md_person` VALUES ('2111585397828644865', '张三', '', NULL, '', '', '', 1, NULL, NULL, 'GH0001', NULL, '18931540689', '', '', 'admin@xuanchen.com', '2008-08-08', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:29:25', 'admin4', '2026-10-18 06:29:25');
INSERT INTO `md_person` VALUES ('2111585678603743233', '李四', '', NULL, '', '', '', 1, NULL, NULL, 'GH0004', NULL, '18931548689', '', '', 'admin4@xuanchen.com', '2008-09-09', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:30:32', 'admin4', '2026-10-18 06:30:32');
INSERT INTO `md_person` VALUES ('2111586696175448065', '王五', '', NULL, '', '', '', 1, NULL, NULL, 'GH0002', NULL, '18931541689', '', '', 'test01@xuanchen.com', '2026-10-18', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:34:34', 'admin4', '2026-10-18 06:34:34');
INSERT INTO `md_person` VALUES ('2111586977894264834', '赵六', '', NULL, '', '', '', 1, NULL, NULL, 'GH0003', NULL, '18931542689', '', '', 'test02@xuanchen.com', '2026-10-18', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:35:42', 'admin4', '2026-10-18 06:42:57');
INSERT INTO `md_person` VALUES ('2111587377011650562', '孙七', '', NULL, '', '', '', 1, NULL, NULL, 'GH0004', NULL, '18931543689', '', '', 'test03@xuanchen.com', '2026-10-18', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:37:17', 'admin4', '2026-10-18 06:43:03');
INSERT INTO `md_person` VALUES ('2111587659510607873', '周八', '', NULL, '', '', '', 1, NULL, NULL, 'GH0005', NULL, '18931544689', '', '', 'test04@xuanchen.com', '2026-10-18', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:38:24', 'admin4', '2026-10-18 06:43:11');
INSERT INTO `md_person` VALUES ('2111587929095303170', '吴九', '', NULL, '', '', '', 1, NULL, NULL, 'GH0006', NULL, '18931545689', '', '', 'test05@xuanchen.com', '2026-10-18', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:39:28', 'admin4', '2026-10-18 06:43:33');
INSERT INTO `md_person` VALUES ('2111588170670436353', '郑十', '', NULL, '', '', '', 1, NULL, NULL, 'GH0007', NULL, '18931546689', '', '', 'test06@xuanchen.com', '2026-10-18', NULL, 1, NULL, 1, 0, 'admin4', '2026-10-18 06:40:26', 'admin4', '2026-10-18 06:43:29');

-- ----------------------------
-- Table structure for md_person_dept
-- ----------------------------
DROP TABLE IF EXISTS `md_person_dept`;
CREATE TABLE `md_person_dept`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `person_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '人员ID',
  `dept_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '部门ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主数据 人员 部门 关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_person_dept
-- ----------------------------
INSERT INTO `md_person_dept` VALUES ('2111585397887365122', '2111585397828644865', 'XC001');
INSERT INTO `md_person_dept` VALUES ('2111585678633103362', '2111585678603743233', 'XC001');
INSERT INTO `md_person_dept` VALUES ('2111586696204808193', '2111586696175448065', 'XC001001');
INSERT INTO `md_person_dept` VALUES ('2111588804555599873', '2111586977894264834', 'XC001002');
INSERT INTO `md_person_dept` VALUES ('2111588830010830850', '2111587377011650562', 'XC001001');
INSERT INTO `md_person_dept` VALUES ('2111588864815165442', '2111587659510607873', 'XC001002');
INSERT INTO `md_person_dept` VALUES ('2111588938030936066', '2111588170670436353', 'XC001001');
INSERT INTO `md_person_dept` VALUES ('2111588955273719809', '2111587929095303170', 'XC001002');

-- ----------------------------
-- Table structure for md_person_post
-- ----------------------------
DROP TABLE IF EXISTS `md_person_post`;
CREATE TABLE `md_person_post`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `person_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '人员ID',
  `post_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '岗位ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主数据 人员 岗位 关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_person_post
-- ----------------------------
INSERT INTO `md_person_post` VALUES ('2111585397916725249', '2111585397828644865', '2010855310054043650');
INSERT INTO `md_person_post` VALUES ('2111585678662463489', '2111585678603743233', '2010855310054043650');
INSERT INTO `md_person_post` VALUES ('2111586696263528450', '2111586696175448065', '2010976279695364098');
INSERT INTO `md_person_post` VALUES ('2111588804555599874', '2111586977894264834', '2010976013071847425');
INSERT INTO `md_person_post` VALUES ('2111588830040190977', '2111587377011650562', '2010976013071847425');
INSERT INTO `md_person_post` VALUES ('2111588864840331266', '2111587659510607873', '2010976815773552641');
INSERT INTO `md_person_post` VALUES ('2111588938060296194', '2111588170670436353', '2010976013071847425');
INSERT INTO `md_person_post` VALUES ('2111588955273719810', '2111587929095303170', '2010976815773552641');

-- ----------------------------
-- Table structure for md_post
-- ----------------------------
DROP TABLE IF EXISTS `md_post`;
CREATE TABLE `md_post`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `post_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `post_description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位描述',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主数据 岗位' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of md_post
-- ----------------------------
INSERT INTO `md_post` VALUES ('2010855310054043650', 'P0001', '总裁', '', 1, 1, 0, 'admin', '2026-01-13 07:24:00', NULL, NULL);
INSERT INTO `md_post` VALUES ('2010976013071847425', 'P0002', '总经理', '', 2, 1, 0, 'admin', '2026-01-13 15:23:38', NULL, NULL);
INSERT INTO `md_post` VALUES ('2010976279695364098', 'P0003', '副总经理', '', 3, 1, 0, 'admin', '2026-01-13 15:24:42', NULL, NULL);
INSERT INTO `md_post` VALUES ('2010976815773552641', 'P0004', '财务', '', 4, 1, 0, 'admin', '2026-01-13 15:26:50', NULL, NULL);

-- ----------------------------
-- Table structure for online_diagram
-- ----------------------------
DROP TABLE IF EXISTS `online_diagram`;
CREATE TABLE `online_diagram`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '雪花ID',
  `doc_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档名称',
  `snapshot` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '流程图快照JSON（AntV X6 graph.toJSON）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_online_dg_user`(`create_by` ASC, `update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '在线文档-流程图架构图' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of online_diagram
-- ----------------------------
INSERT INTO `online_diagram` VALUES ('2116892323139547137', '流程图-测试001', '{\"cells\":[{\"shape\":\"flow-edge\",\"zIndex\":0,\"labels\":[{\"attrs\":{\"labelBody\":{\"fill\":\"#fff\",\"stroke\":\"#D8DCE6\",\"strokeWidth\":1,\"rx\":3,\"ry\":3},\"labelText\":{\"fill\":\"#4E5969\",\"fontSize\":11}}}],\"id\":\"0c3c36f5-db6c-4e65-826a-a7c0964d3b1c\",\"source\":{\"cell\":\"c3e16d50-9b0b-4e4a-a7f5-eede3add21bb\",\"port\":\"8524fe3e-0c18-44df-9f4c-b773ae3ea588\"},\"target\":{\"cell\":\"788ebf24-2691-4525-a005-121a1c9ebda5\",\"port\":\"0a9605d1-079e-4ef3-9f77-87c5bb3d4caa\"}},{\"shape\":\"flow-edge\",\"zIndex\":0,\"labels\":[{\"attrs\":{\"labelBody\":{\"fill\":\"#fff\",\"stroke\":\"#D8DCE6\",\"strokeWidth\":1,\"rx\":3,\"ry\":3},\"labelText\":{\"fill\":\"#4E5969\",\"fontSize\":11}}}],\"id\":\"667437d7-682b-411a-ae6b-9811f511f5dc\",\"source\":{\"cell\":\"788ebf24-2691-4525-a005-121a1c9ebda5\",\"port\":\"3e1e71a2-31d7-47c9-bfe6-f5e36a2ee8f2\"},\"target\":{\"cell\":\"d3be4f38-bfe7-4c85-bf24-c78dc4528d48\",\"port\":\"6b5fbe04-4375-4f9a-8baf-161144e6ea86\"}},{\"shape\":\"flow-edge\",\"zIndex\":0,\"labels\":[{\"attrs\":{\"labelBody\":{\"fill\":\"#fff\",\"stroke\":\"#D8DCE6\",\"strokeWidth\":1,\"rx\":3,\"ry\":3},\"labelText\":{\"fill\":\"#4E5969\",\"fontSize\":11}}}],\"id\":\"f9f11e76-7a42-4b21-bdd7-f724630a75f5\",\"source\":{\"cell\":\"d3be4f38-bfe7-4c85-bf24-c78dc4528d48\",\"port\":\"c7bfb63d-ec55-4db9-8bc7-107dc59572b1\"},\"target\":{\"cell\":\"7dc6da11-ebbe-4197-8360-ef4a6dbad011\",\"port\":\"0a9605d1-079e-4ef3-9f77-87c5bb3d4caa\"}},{\"shape\":\"flow-edge\",\"zIndex\":0,\"labels\":[{\"attrs\":{\"labelBody\":{\"fill\":\"#fff\",\"stroke\":\"#D8DCE6\",\"strokeWidth\":1,\"rx\":3,\"ry\":3},\"labelText\":{\"fill\":\"#4E5969\",\"fontSize\":11}}}],\"id\":\"03de9e89-aa84-4b03-8245-65ef422c91c5\",\"source\":{\"cell\":\"7dc6da11-ebbe-4197-8360-ef4a6dbad011\",\"port\":\"3e1e71a2-31d7-47c9-bfe6-f5e36a2ee8f2\"},\"target\":{\"cell\":\"ff7f5ee1-69cb-4d9f-ba13-1ac1ef15e6c7\",\"port\":\"39fdbf08-1849-4267-8c03-ed15335c66b1\"}},{\"position\":{\"x\":-490,\"y\":-140},\"size\":{\"width\":72,\"height\":40},\"visible\":true,\"shape\":\"flow-start\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}}},\"items\":[{\"group\":\"top\",\"id\":\"268541b6-fd88-489e-92f0-233340784d60\"},{\"group\":\"right\",\"id\":\"8524fe3e-0c18-44df-9f4c-b773ae3ea588\"},{\"group\":\"bottom\",\"id\":\"5270038f-addf-4a5d-b84c-8970d28f8724\"},{\"group\":\"left\",\"id\":\"3d37bd80-72d1-4ed4-a902-11dcaac9cb2a\"}]},\"id\":\"c3e16d50-9b0b-4e4a-a7f5-eede3add21bb\",\"zIndex\":1},{\"position\":{\"x\":120,\"y\":-140},\"size\":{\"width\":72,\"height\":40},\"visible\":true,\"shape\":\"flow-end\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}}},\"items\":[{\"group\":\"top\",\"id\":\"59aff425-1c42-425f-b053-560b3adb0b09\"},{\"group\":\"right\",\"id\":\"405a07ac-167f-40a9-89b8-8aa26af532e0\"},{\"group\":\"bottom\",\"id\":\"40d708a5-f409-4d13-b420-4217a347c287\"},{\"group\":\"left\",\"id\":\"39fdbf08-1849-4267-8c03-ed15335c66b1\"}]},\"id\":\"ff7f5ee1-69cb-4d9f-ba13-1ac1ef15e6c7\",\"zIndex\":2},{\"position\":{\"x\":-340,\"y\":-140},\"size\":{\"width\":84,\"height\":40},\"visible\":true,\"shape\":\"flow-process\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}}},\"items\":[{\"group\":\"top\",\"id\":\"368d0072-1fcc-4372-a534-0229c09bc527\"},{\"group\":\"right\",\"id\":\"3e1e71a2-31d7-47c9-bfe6-f5e36a2ee8f2\"},{\"group\":\"bottom\",\"id\":\"84ee1d0d-fbfa-4eb6-98b6-57d277b9b3d2\"},{\"group\":\"left\",\"id\":\"0a9605d1-079e-4ef3-9f77-87c5bb3d4caa\"}]},\"id\":\"788ebf24-2691-4525-a005-121a1c9ebda5\",\"zIndex\":3},{\"position\":{\"x\":-190,\"y\":-147},\"size\":{\"width\":88,\"height\":54},\"visible\":true,\"shape\":\"flow-decision\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}}},\"items\":[{\"group\":\"top\",\"id\":\"24e7fde2-89c4-4589-9e03-5fa5824e2dd8\"},{\"group\":\"right\",\"id\":\"c7bfb63d-ec55-4db9-8bc7-107dc59572b1\"},{\"group\":\"bottom\",\"id\":\"0d4d2596-d3c8-4d4b-9291-59a7b6b89479\"},{\"group\":\"left\",\"id\":\"6b5fbe04-4375-4f9a-8baf-161144e6ea86\"}]},\"id\":\"d3be4f38-bfe7-4c85-bf24-c78dc4528d48\",\"zIndex\":4},{\"position\":{\"x\":-35,\"y\":-140},\"size\":{\"width\":84,\"height\":40},\"visible\":true,\"shape\":\"flow-process\",\"ports\":{\"groups\":{\"top\":{\"position\":\"top\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"right\":{\"position\":\"right\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"bottom\":{\"position\":\"bottom\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}},\"left\":{\"position\":\"left\",\"attrs\":{\"circle\":{\"r\":4,\"magnet\":true,\"stroke\":\"#5F95FF\",\"strokeWidth\":1,\"fill\":\"#fff\",\"style\":{\"visibility\":\"hidden\"}}}}},\"items\":[{\"group\":\"top\",\"id\":\"368d0072-1fcc-4372-a534-0229c09bc527\"},{\"group\":\"right\",\"id\":\"3e1e71a2-31d7-47c9-bfe6-f5e36a2ee8f2\"},{\"group\":\"bottom\",\"id\":\"84ee1d0d-fbfa-4eb6-98b6-57d277b9b3d2\"},{\"group\":\"left\",\"id\":\"0a9605d1-079e-4ef3-9f77-87c5bb3d4caa\"}]},\"id\":\"7dc6da11-ebbe-4197-8360-ef4a6dbad011\",\"zIndex\":5}]}', 0, 'admin4', '2026-11-01 21:57:14', 'admin4', '2026-11-01 22:00:00');

-- ----------------------------
-- Table structure for online_form
-- ----------------------------
DROP TABLE IF EXISTS `online_form`;
CREATE TABLE `online_form`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '雪花ID',
  `form_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表单名称',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `schema_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '表单设计JSON（字段列表：type/label/key/placeholder/required/options）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_online_form_user`(`create_by` ASC, `update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '在线表单（流程审批，可被流程定义绑定）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of online_form
-- ----------------------------
INSERT INTO `online_form` VALUES ('2116893774049964034', '在线表单-测试001', '', '{\"fields\":[{\"type\":\"input\",\"label\":\"单行输入1\",\"key\":\"input1\",\"placeholder\":\"\",\"required\":false,\"span\":6},{\"type\":\"input\",\"label\":\"单行输入2\",\"key\":\"input2\",\"placeholder\":\"\",\"required\":false,\"span\":6},{\"type\":\"input\",\"label\":\"单行输入3\",\"key\":\"input3\",\"placeholder\":\"\",\"required\":false,\"span\":6},{\"type\":\"input\",\"label\":\"单行输入4\",\"key\":\"input4\",\"placeholder\":\"\",\"required\":false,\"span\":6},{\"type\":\"textarea\",\"label\":\"多行输入\",\"key\":\"textarea5\",\"placeholder\":\"\",\"required\":false,\"span\":24},{\"type\":\"number\",\"label\":\"数字\",\"key\":\"number6\",\"placeholder\":\"\",\"required\":false,\"span\":12},{\"type\":\"date\",\"label\":\"日期\",\"key\":\"date7\",\"placeholder\":\"\",\"required\":false,\"span\":12},{\"type\":\"select\",\"label\":\"下拉选择\",\"key\":\"select8\",\"placeholder\":\"\",\"required\":false,\"span\":12,\"options\":[\"选项1\",\"选项2\",\"选项3\"]},{\"type\":\"radio\",\"label\":\"单选\",\"key\":\"radio9\",\"placeholder\":\"\",\"required\":false,\"span\":12,\"options\":[\"选项1\",\"选项2\",\"选项3\"]},{\"type\":\"checkbox\",\"label\":\"多选\",\"key\":\"checkbox10\",\"placeholder\":\"\",\"required\":false,\"span\":12,\"options\":[\"选项1\",\"选项2\",\"选项3\"]},{\"type\":\"switch\",\"label\":\"开关\",\"key\":\"switch11\",\"placeholder\":\"\",\"required\":false,\"span\":12}]}', 0, 'admin4', '2026-11-01 22:03:00', 'admin4', '2026-11-01 22:04:25');

-- ----------------------------
-- Table structure for online_mindmap
-- ----------------------------
DROP TABLE IF EXISTS `online_mindmap`;
CREATE TABLE `online_mindmap`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '雪花ID',
  `doc_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档名称',
  `snapshot` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '思维导图快照JSON（simple-mind-map getData）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_online_mm_user`(`create_by` ASC, `update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '在线文档-思维导图' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of online_mindmap
-- ----------------------------
INSERT INTO `online_mindmap` VALUES ('2116891899250601985', '思维导图-测试001', '{\"data\":{\"text\":\"思维导图-测试001\",\"expand\":true,\"uid\":\"f5fc3025-aeab-42fd-b2df-26b9d21327ef\",\"isActive\":false},\"children\":[{\"data\":{\"text\":\"二级节点1\",\"uid\":\"4923cc83-81ec-4a97-b784-77014f68676e\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[{\"data\":{\"text\":\"分支主题\",\"uid\":\"912d7bc2-69cf-47e3-a6af-87c1eac02c21\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[{\"data\":{\"text\":\"分支主题\",\"uid\":\"d4241245-a10d-4014-848c-a3deb1dd1454\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[]}]},{\"data\":{\"text\":\"分支主题\",\"expand\":true,\"richText\":false,\"isActive\":false,\"uid\":\"feb54549-a79b-4346-95b5-26d9b44d75e2\"},\"children\":[{\"data\":{\"text\":\"分支主题\",\"uid\":\"108f5b6d-9b24-4d56-b63c-b49eb4c606d1\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[{\"data\":{\"text\":\"分支主题\",\"uid\":\"8e06c61c-1160-42da-a222-39540408dd12\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[]}]}]}]},{\"data\":{\"text\":\"二级节点2\",\"expand\":true,\"richText\":false,\"isActive\":false,\"uid\":\"8317841c-20df-4162-8b85-73a87bf272cb\"},\"children\":[{\"data\":{\"text\":\"分支主题\",\"uid\":\"010dec47-a04d-469f-a59e-103af85814ea\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[{\"data\":{\"text\":\"分支主题\",\"uid\":\"d7e70c8a-fd03-4b09-93f9-4edde82cd790\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[]}]},{\"data\":{\"text\":\"分支主题\",\"expand\":true,\"richText\":false,\"isActive\":false,\"uid\":\"c769f19f-9bc1-4d68-b055-187796e11b9e\"},\"children\":[{\"data\":{\"text\":\"分支主题\",\"uid\":\"b3086095-6191-4f45-abe1-af978aff2638\",\"expand\":true,\"richText\":false,\"isActive\":false},\"children\":[]}]}]}],\"smmVersion\":\"0.14.0-fix.3\"}', 0, 'admin4', '2026-11-01 21:55:33', 'admin4', '2026-11-01 21:56:39');

-- ----------------------------
-- Table structure for rpt_dataset
-- ----------------------------
DROP TABLE IF EXISTS `rpt_dataset`;
CREATE TABLE `rpt_dataset`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `template_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属模板ID（设计整体在「模板设计」，数据集随模板复用）',
  `ds_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '引用数据源ID（为空=使用系统主库）',
  `dataset_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据集编码（模板内唯一）',
  `dataset_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据集名称',
  `sql_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '查询 SQL（仅 SELECT，执行前经 SqlGuard 校验）',
  `params_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '参数定义 JSON 数组',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rpt_dataset_template`(`template_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '报表数据集' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of rpt_dataset
-- ----------------------------
INSERT INTO `rpt_dataset` VALUES ('2116896191269302274', '2116895747914592258', NULL, 'mdPerson', '人员', 'SELECT\n  real_name,\n  mobile\nFROM\n  md_person', NULL, 0, 'admin4', '2026-11-01 22:12:37', 'admin4', '2026-11-01 22:15:58');

-- ----------------------------
-- Table structure for rpt_report
-- ----------------------------
DROP TABLE IF EXISTS `rpt_report`;
CREATE TABLE `rpt_report`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `report_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报表编码（查看页路由用，全库唯一）',
  `report_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报表名称',
  `report_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '报表类型（1明细表 2汇总表 3自由格式）',
  `template_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源模板ID（新增时选择，创建时复制模板快照，之后解耦）',
  `config_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '报表配置 JSON（Univer snapshot + 参数定义 + 单元格绑定）',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态（0草稿 1已发布 2停用）',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rpt_report_code`(`report_code` ASC) USING BTREE,
  INDEX `idx_rpt_report_template`(`template_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '报表主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of rpt_report
-- ----------------------------
INSERT INTO `rpt_report` VALUES ('2116897535627620353', 'BB_TEST001', '报表-测试001', '1', '2116895747914592258', '{\"snapshot\":{\"id\":\"report-template\",\"sheetOrder\":[\"sheet1\"],\"name\":\"报表模板\",\"appVersion\":\"1.1.0\",\"locale\":\"zhCN\",\"styles\":{},\"sheets\":{\"sheet1\":{\"id\":\"sheet1\",\"name\":\"Sheet1\",\"rowCount\":60,\"columnCount\":20,\"cellData\":{},\"tabColor\":\"\",\"hidden\":0,\"zoomRatio\":1,\"freeze\":{\"startRow\":-1,\"startColumn\":-1,\"ySplit\":0,\"xSplit\":0},\"scrollTop\":0,\"scrollLeft\":0,\"defaultColumnWidth\":88,\"defaultRowHeight\":24,\"mergeData\":[],\"rowData\":{},\"columnData\":{},\"showGridlines\":1,\"rowHeader\":{\"width\":46,\"hidden\":0},\"columnHeader\":{\"height\":20,\"hidden\":0},\"rightToLeft\":0}},\"resources\":[{\"name\":\"SHEET_RANGE_PROTECTION_PLUGIN\",\"data\":\"\"},{\"name\":\"SHEET_AuthzIoMockService_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_WORKSHEET_PROTECTION_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_WORKSHEET_PROTECTION_POINT_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_DEFINED_NAME_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_RANGE_THEME_MODEL_PLUGIN\",\"data\":\"{}\"}],\"editable\":true,\"calculationSettings\":{\"iterativeCalculation\":false,\"maxIterations\":100,\"maxChange\":0.001}},\"params\":[],\"bindings\":{\"list:A1:T1\":{\"type\":\"list\",\"anchor\":\"A1\",\"range\":\"A1:T1\",\"dataset\":\"mdPerson\",\"columns\":{\"A\":\"real_name\",\"B\":\"mobile\"}}}}', 0, NULL, '', 0, 'admin4', '2026-11-01 22:17:57', 'admin4', '2026-11-01 22:17:57');

-- ----------------------------
-- Table structure for rpt_template
-- ----------------------------
DROP TABLE IF EXISTS `rpt_template`;
CREATE TABLE `rpt_template`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板编码（全库唯一）',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `template_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '模板类型（1明细表 2汇总表 3自由格式）',
  `config_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '模板配置 JSON（Univer snapshot + 参数定义 + 单元格绑定）',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rpt_template_code`(`template_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '报表模板' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of rpt_template
-- ----------------------------
INSERT INTO `rpt_template` VALUES ('2116895747914592258', 'BBMB_TEST001', '报表模板-测试001', '1', '{\"snapshot\":{\"id\":\"report-template\",\"sheetOrder\":[\"sheet1\"],\"name\":\"报表模板\",\"appVersion\":\"1.1.0\",\"locale\":\"zhCN\",\"styles\":{},\"sheets\":{\"sheet1\":{\"id\":\"sheet1\",\"name\":\"Sheet1\",\"rowCount\":60,\"columnCount\":20,\"cellData\":{},\"tabColor\":\"\",\"hidden\":0,\"zoomRatio\":1,\"freeze\":{\"startRow\":-1,\"startColumn\":-1,\"ySplit\":0,\"xSplit\":0},\"scrollTop\":0,\"scrollLeft\":0,\"defaultColumnWidth\":88,\"defaultRowHeight\":24,\"mergeData\":[],\"rowData\":{},\"columnData\":{},\"showGridlines\":1,\"rowHeader\":{\"width\":46,\"hidden\":0},\"columnHeader\":{\"height\":20,\"hidden\":0},\"rightToLeft\":0}},\"resources\":[{\"name\":\"SHEET_RANGE_PROTECTION_PLUGIN\",\"data\":\"\"},{\"name\":\"SHEET_AuthzIoMockService_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_WORKSHEET_PROTECTION_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_WORKSHEET_PROTECTION_POINT_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_DEFINED_NAME_PLUGIN\",\"data\":\"{}\"},{\"name\":\"SHEET_RANGE_THEME_MODEL_PLUGIN\",\"data\":\"{}\"}],\"editable\":true,\"calculationSettings\":{\"iterativeCalculation\":false,\"maxIterations\":100,\"maxChange\":0.001}},\"params\":[],\"bindings\":{\"list:A1:T1\":{\"type\":\"list\",\"anchor\":\"A1\",\"range\":\"A1:T1\",\"dataset\":\"mdPerson\",\"columns\":{\"A\":\"real_name\",\"B\":\"mobile\"}}}}', 1, '', 0, 'admin4', '2026-11-01 22:10:51', 'admin4', '2026-11-01 22:16:25');

-- ----------------------------
-- Table structure for sys_account
-- ----------------------------
DROP TABLE IF EXISTS `sys_account`;
CREATE TABLE `sys_account`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `person_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人员ID',
  `user_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名（登录名）',
  `nick_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称（系统内展示名）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号（登录凭证/短信验证）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱（找回密码/通知）',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `pwd_reset_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否要求下次登录修改密码（1是0否）',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '账号状态（1正常2冻结）',
  `salt` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '盐值（sb3用）',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户账号表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_account
-- ----------------------------
INSERT INTO `sys_account` VALUES ('1757768950749499393', '2111585397828644865', 'admin', '管理员', NULL, '18931540689', 'admin@xuanchen.com', '0c633818ae54281006d5ef2ee7559d6af1df38d4fe93aea77c733adc9a817df2d3149ce6c7e053e97d67de3d028ec61c921e4b4b6b66e3b787e193bb0a6e5d39', 0, 1, 'hBOpLQrn', NULL, 0, 'super', '2024-02-14 22:09:10', 'admin4', '2026-10-18 06:30:50');
INSERT INTO `sys_account` VALUES ('1757769284339273730', '2111586696175448065', 'test01', '测试01', NULL, '18931541689', 'test01@xuanchen.com', '77a399ac55ae7b8400f27ef6c7d936d623bf54137a6c356cd96d97d14895dcae756e60f1cfd70bc3b2069800f206950ef1ef13169898df979b034217af528acb', 0, 1, 'RAtMIWos', NULL, 0, 'admin', '2024-02-14 22:10:30', 'admin4', '2026-10-18 06:35:52');
INSERT INTO `sys_account` VALUES ('1757769358909804546', '2111586977894264834', 'test02', '测试02', NULL, '18931542689', 'test02@xuanchen.com', '9e83250f13f5ad1d60c468602bdc399237cfa5462d1f431fca9726a888f14fb086ab3abba7197b420397f23cfa87f072f57afb3b77d8b343e0c4a434861d2b52', 0, 1, 'icXXjqXS', NULL, 0, 'admin', '2024-02-14 22:10:48', 'admin4', '2026-10-18 06:36:02');
INSERT INTO `sys_account` VALUES ('1772395839434694658', '2111587377011650562', 'test03', '测试03', NULL, '18931543689', 'test03@xuanchen.com', 'f867ff114a35884dce6879007c0d6f80ed5215cd584bc21aa94425dc5795216d7d2b6fc7110abd503eba305eeed65bbd165093ee0742346b6a20170ae8f8c2d5', 0, 1, 'WV!cdspi', NULL, 0, NULL, '2024-03-26 06:51:12', 'admin4', '2026-10-18 06:37:26');
INSERT INTO `sys_account` VALUES ('1772396269187276802', '2111587659510607873', 'test04', '测试04', NULL, '18931544689', 'test04@xuanchen.com', '8adc7121a7680e80c2bf7220d04e025ecdf5fca6110f603c29c4e94cf2873d161d5cf22b14ac30d0d7d58cea9000e7bd9a595f09b2e67514579932cf6d0b8bd8', 0, 1, 'fucgEGEe', NULL, 0, NULL, '2024-03-26 06:52:55', 'admin4', '2026-10-18 06:38:32');
INSERT INTO `sys_account` VALUES ('1779508954504761345', '2111587929095303170', 'test05', '测试05', NULL, '18931545689', 'test05@xuanchen.com', 'dfccc65fb09352a3f7fb7e971d96aaa9feafbc38fd5d61815ef2dc8c721743b009e4bcdc9c19cef1805a905157ee8fb66d588334803d93519608a9117631aacd', 0, 1, 'cFy&xQYO', NULL, 0, 'admin', '2024-04-14 21:56:11', 'admin4', '2026-10-18 06:39:43');
INSERT INTO `sys_account` VALUES ('1779511801023696897', '2111588170670436353', 'test06', '测试06', NULL, '18931546689', 'test06@xuanchen.com', 'd67ed4a624a53499d0bfa739aaf4e59071314a9cf1dd9cf59a17577a6d80d8d5f90c2dab9e3819b81321532261fea439d0869869961c5a0e980c98941389cc61', 0, 1, 'iRbcpUG%', NULL, 0, 'admin', '2024-04-14 22:07:30', 'admin4', '2026-10-18 06:40:35');
INSERT INTO `sys_account` VALUES ('1779511801023696898', '2111585678603743233', 'admin4', '管理员4', NULL, '18931548689', 'admin4@xuanchen.com', '$2a$10$lSSiQQ4P1atDCM89UxYZJOMWLSeseVtecdmIifTJNCzRJ1dbPi6EW', 0, 1, NULL, NULL, 0, 'super', '2026-03-27 09:48:15', 'admin4', '2026-10-18 06:30:43');

-- ----------------------------
-- Table structure for sys_account_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_account_role`;
CREATE TABLE `sys_account_role`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `account_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '账号ID',
  `role_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '角色ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账号角色关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_account_role
-- ----------------------------
INSERT INTO `sys_account_role` VALUES ('2111585725693194242', '1779511801023696898', '1750881083171422209');
INSERT INTO `sys_account_role` VALUES ('2111585755195928578', '1757768950749499393', '1750881083171422209');
INSERT INTO `sys_account_role` VALUES ('2111587021720547330', '1757769284339273730', '1745438525662351362');
INSERT INTO `sys_account_role` VALUES ('2111587062178803713', '1757769358909804546', '1745438525662351362');
INSERT INTO `sys_account_role` VALUES ('2111587414957518850', '1772395839434694658', '1745438525662351362');
INSERT INTO `sys_account_role` VALUES ('2111587694751150081', '1772396269187276802', '1745438525662351362');
INSERT INTO `sys_account_role` VALUES ('2111587989258399746', '1779508954504761345', '1745438525662351362');
INSERT INTO `sys_account_role` VALUES ('2111588209111232513', '1779511801023696897', '1745438525662351362');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `config_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '参数名称',
  `config_description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数描述',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数键名',
  `config_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数键值',
  `config_type` tinyint NULL DEFAULT NULL COMMENT '参数类型(1系统2业务)',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '参数配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES ('2015795773986979841', '验证码开关', '是否启用验证码true启用false关闭', 'captchaEnabled', 'true', 1, 1, 1, 0, 'admin', '2026-01-26 22:35:39', 'admin', '2026-02-14 22:51:18');
INSERT INTO `sys_config` VALUES ('2015795773986979842', '验证码类型', '验证码背景图生成方式：dynamic动态渐变生成，static使用预设静态图片', 'captchaType', 'dynamic', 1, 2, 1, 0, 'admin', '2026-10-10 20:00:00', 'admin', '2026-10-10 20:00:00');

-- ----------------------------
-- Table structure for sys_datasource
-- ----------------------------
DROP TABLE IF EXISTS `sys_datasource`;
CREATE TABLE `sys_datasource`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `ds_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `ds_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源类型（mysql/mariadb/postgresql/oracle/sqlserver/dm/kingbase/h2）',
  `jdbc_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'JDBC 连接串（支持 mysql/mariadb/postgresql/oracle/sqlserver/dm/kingbase/h2 协议）',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码（AES/GCM 加密存储，密钥 xuanchen.sys.encrypt-key）',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '统一数据源（报表/API/大屏/代码生成共用）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_datasource
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `dict_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典编码',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典名称',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES ('1988219272327151617', 'yes_no', '是否', 1, 1, 0, 'admin', '2025-11-11 20:16:29', NULL, NULL);
INSERT INTO `sys_dict` VALUES ('2012158527658344450', 'config_type', '参数类型', 3, 1, 0, 'admin', '2026-01-16 21:42:32', 'admin', '2026-02-14 22:52:43');
INSERT INTO `sys_dict` VALUES ('2015181385600995330', 'account_status', '账号状态', 4, 1, 0, 'admin', '2026-01-25 05:54:17', 'admin', '2026-02-14 22:52:47');
INSERT INTO `sys_dict` VALUES ('2022685399402033153', 'succ_fail', '成功失败', 2, 1, 0, 'admin', '2026-02-14 22:52:34', NULL, NULL);
INSERT INTO `sys_dict` VALUES ('2026841705318932481', 'db_conn_type', '数据库连接类型', 5, 1, 0, 'admin', '2026-02-26 10:08:14', NULL, NULL);
INSERT INTO `sys_dict` VALUES ('2026844564127178753', 'db_table_column_type', '数据库表字段类型', 6, 1, 0, 'admin', '2026-02-26 10:19:36', NULL, NULL);
INSERT INTO `sys_dict` VALUES ('2026846530785992706', 'ui_control_type', '前端控件类型', 7, 1, 0, 'admin', '2026-02-26 10:27:25', NULL, NULL);
INSERT INTO `sys_dict` VALUES ('2069281000000000001', 'del_flag', '删除状态', 8, 1, 0, 'admin', '2026-09-28 14:00:00', NULL, NULL);
INSERT INTO `sys_dict` VALUES ('2110984292941885441', 'gender', '性别', 9, 1, 0, 'admin4', '2026-10-16 14:40:50', 'admin4', '2026-10-16 14:40:50');
INSERT INTO `sys_dict` VALUES ('2110984511192494081', 'marital_status', '婚姻状况', 10, 1, 0, 'admin4', '2026-10-16 14:41:42', 'admin4', '2026-10-16 14:41:57');
INSERT INTO `sys_dict` VALUES ('2110984843473645570', 'education', '学历', 11, 1, 0, 'admin4', '2026-10-16 14:43:02', 'admin4', '2026-10-16 14:43:02');
INSERT INTO `sys_dict` VALUES ('2110985368617283586', 'person_type', '人员类型', 12, 1, 0, 'admin4', '2026-10-16 14:45:07', 'admin4', '2026-10-16 14:45:07');
INSERT INTO `sys_dict` VALUES ('2111180307926011906', 'person_status', '人员状态', 13, 1, 0, 'admin4', '2026-10-17 03:39:44', 'admin4', '2026-10-17 03:39:44');

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `dict_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典编码',
  `dict_item_text` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典文本',
  `dict_item_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典值',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO `sys_dict_item` VALUES ('1988219316371537922', 'yes_no', '是', '1', 1, 1, 0, 'admin', '2025-11-11 20:16:39', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1988219354069942274', 'yes_no', '否', '0', 2, 1, 0, 'admin', '2025-11-11 20:16:48', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2012158582834413569', 'config_type', '系统', '1', 1, 1, 0, 'admin', '2026-01-16 21:42:45', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2012158635976245250', 'config_type', '业务', '2', 2, 1, 0, 'admin', '2026-01-16 21:42:58', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2015181432967270402', 'account_status', '正常', '1', 1, 1, 0, 'admin', '2026-01-25 05:54:29', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2015181480673284097', 'account_status', '冻结', '2', 2, 1, 0, 'admin', '2026-01-25 05:54:40', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2022685559305678850', 'succ_fail', '成功', '1', 1, 1, 0, 'admin', '2026-02-14 22:53:12', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2022685602842554369', 'succ_fail', '失败', '0', 2, 1, 0, 'admin', '2026-02-14 22:53:22', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026841998664359937', 'db_conn_type', 'MySQL', 'mysql', 1, 1, 0, 'admin', '2026-02-26 10:09:24', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026842286813044738', 'db_conn_type', 'PostgreSQL', 'postgresql', 2, 1, 0, 'admin', '2026-02-26 10:10:33', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026843088566837249', 'db_conn_type', 'SQL Server', 'sqlserver', 3, 1, 0, 'admin', '2026-02-26 10:13:44', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026843177993592833', 'db_conn_type', 'Oracle', 'oracle', 4, 1, 0, 'admin', '2026-02-26 10:14:05', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026843269253259266', 'db_conn_type', 'SQLite', 'sqlite', 5, 1, 0, 'admin', '2026-02-26 10:14:27', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026843366229762049', 'db_conn_type', 'MariaDB', 'mariadb', 6, 1, 0, 'admin', '2026-02-26 10:14:50', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026843487893938178', 'db_conn_type', 'MongoDB', 'mongodb', 7, 1, 0, 'admin', '2026-02-26 10:15:19', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026843554365267970', 'db_conn_type', 'Redis', 'redis', 8, 1, 0, 'admin', '2026-02-26 10:15:35', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026844723154214914', 'db_table_column_type', 'String', 'string', 1, 1, 0, 'admin', '2026-02-26 10:20:14', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026844802783076353', 'db_table_column_type', 'Integer', 'integer', 2, 1, 0, 'admin', '2026-02-26 10:20:33', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026844852905009153', 'db_table_column_type', 'Double', 'double', 3, 1, 0, 'admin', '2026-02-26 10:20:45', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026844889617752066', 'db_table_column_type', 'Date', 'date', 4, 1, 0, 'admin', '2026-02-26 10:20:53', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026845480372887554', 'db_table_column_type', 'Datetime', 'datetime', 5, 1, 0, 'admin', '2026-02-26 10:23:14', 'admin', '2026-02-26 10:23:47');
INSERT INTO `sys_dict_item` VALUES ('2026845590905380865', 'db_table_column_type', 'BigDecimal', 'bigdecimal', 6, 1, 0, 'admin', '2026-02-26 10:23:41', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026845661281607681', 'db_table_column_type', 'Text', 'text', 7, 1, 0, 'admin', '2026-02-26 10:23:57', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026845720861696001', 'db_table_column_type', 'Blob', 'blob', 8, 1, 0, 'admin', '2026-02-26 10:24:11', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026846966465126401', 'ui_control_type', '多选框', 'Checkbox', 1, 1, 0, 'admin', '2026-02-26 10:29:08', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847064116912129', 'ui_control_type', '日期选择框', 'DatePicker', 2, 1, 0, 'admin', '2026-02-26 10:29:32', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847154495774722', 'ui_control_type', '输入框', 'Input', 3, 1, 0, 'admin', '2026-02-26 10:29:53', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847231180234754', 'ui_control_type', '数字输入框', 'InputNumber', 4, 1, 0, 'admin', '2026-02-26 10:30:12', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847308724527105', 'ui_control_type', '单选框', 'Radio', 5, 1, 0, 'admin', '2026-02-26 10:30:30', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847400722391041', 'ui_control_type', '选择器', 'Select', 6, 1, 0, 'admin', '2026-02-26 10:30:52', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847532792635393', 'ui_control_type', '开关', 'Switch', 7, 1, 0, 'admin', '2026-02-26 10:31:23', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847639797719042', 'ui_control_type', '时间选择框', 'TimePicker', 8, 1, 0, 'admin', '2026-02-26 10:31:49', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847740272271361', 'ui_control_type', '树选择', 'TreeSelect', 9, 1, 0, 'admin', '2026-02-26 10:32:13', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2026847819431370754', 'ui_control_type', '上传', 'Upload', 10, 1, 0, 'admin', '2026-02-26 10:32:32', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2069281000000000002', 'del_flag', '正常', '0', 1, 1, 0, 'admin', '2026-09-28 14:00:00', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2069281000000000003', 'del_flag', '已删除', '1', 2, 1, 0, 'admin', '2026-09-28 14:00:00', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2110984358213644290', 'gender', '男', '1', 1, 1, 0, 'admin4', '2026-10-16 14:41:06', 'admin4', '2026-10-16 14:41:06');
INSERT INTO `sys_dict_item` VALUES ('2110984389498957825', 'gender', '女', '2', 2, 1, 0, 'admin4', '2026-10-16 14:41:13', 'admin4', '2026-10-16 14:41:13');
INSERT INTO `sys_dict_item` VALUES ('2110984632978305025', 'marital_status', '未婚', '1', 1, 1, 0, 'admin4', '2026-10-16 14:42:11', 'admin4', '2026-10-16 14:42:11');
INSERT INTO `sys_dict_item` VALUES ('2110984664880181250', 'marital_status', '已婚', '2', 2, 1, 0, 'admin4', '2026-10-16 14:42:19', 'admin4', '2026-10-16 14:42:19');
INSERT INTO `sys_dict_item` VALUES ('2110984700594679810', 'marital_status', '离异', '3', 3, 1, 0, 'admin4', '2026-10-16 14:42:27', 'admin4', '2026-10-16 14:42:27');
INSERT INTO `sys_dict_item` VALUES ('2110984733608046594', 'marital_status', '丧偶', '4', 4, 1, 0, 'admin4', '2026-10-16 14:42:35', 'admin4', '2026-10-16 14:42:35');
INSERT INTO `sys_dict_item` VALUES ('2110984890105917442', 'education', '小学', '1', 1, 1, 0, 'admin4', '2026-10-16 14:43:13', 'admin4', '2026-10-16 14:43:13');
INSERT INTO `sys_dict_item` VALUES ('2110985107530248194', 'education', '初中', '2', 2, 1, 0, 'admin4', '2026-10-16 14:44:05', 'admin4', '2026-10-16 14:44:05');
INSERT INTO `sys_dict_item` VALUES ('2110985151377502209', 'education', '高中', '3', 3, 1, 0, 'admin4', '2026-10-16 14:44:15', 'admin4', '2026-10-16 14:44:15');
INSERT INTO `sys_dict_item` VALUES ('2110985190703296513', 'education', '大专', '4', 4, 1, 0, 'admin4', '2026-10-16 14:44:24', 'admin4', '2026-10-16 14:44:24');
INSERT INTO `sys_dict_item` VALUES ('2110985228133265409', 'education', '本科', '5', 5, 1, 0, 'admin4', '2026-10-16 14:44:33', 'admin4', '2026-10-16 14:44:33');
INSERT INTO `sys_dict_item` VALUES ('2110985259619905537', 'education', '研究生', '6', 6, 1, 0, 'admin4', '2026-10-16 14:44:41', 'admin4', '2026-10-16 14:44:41');
INSERT INTO `sys_dict_item` VALUES ('2110985443263311873', 'person_type', '正式工', '1', 1, 1, 0, 'admin4', '2026-10-16 14:45:25', 'admin4', '2026-10-16 14:45:25');
INSERT INTO `sys_dict_item` VALUES ('2110985521520635905', 'person_type', '外协工', '2', 2, 1, 0, 'admin4', '2026-10-16 14:45:43', 'admin4', '2026-10-16 14:45:43');
INSERT INTO `sys_dict_item` VALUES ('2110985561794342914', 'person_type', '临时工', '3', 3, 1, 0, 'admin4', '2026-10-16 14:45:53', 'admin4', '2026-10-16 14:45:53');
INSERT INTO `sys_dict_item` VALUES ('2111180596120834049', 'person_status', '在职', '1', 1, 1, 0, 'admin4', '2026-10-17 03:40:53', 'admin4', '2026-10-17 03:40:53');
INSERT INTO `sys_dict_item` VALUES ('2111180640840503297', 'person_status', '试用', '2', 2, 1, 0, 'admin4', '2026-10-17 03:41:03', 'admin4', '2026-10-17 03:41:03');
INSERT INTO `sys_dict_item` VALUES ('2111180677419028482', 'person_status', '离职', '3', 3, 1, 0, 'admin4', '2026-10-17 03:41:12', 'admin4', '2026-10-17 03:41:12');
INSERT INTO `sys_dict_item` VALUES ('2111180714828025857', 'person_status', '退休', '4', 4, 1, 0, 'admin4', '2026-10-17 03:41:21', 'admin4', '2026-10-17 03:41:21');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单ID',
  `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父菜单ID',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由地址',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '路由名称',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `is_leaf` tinyint(1) NULL DEFAULT NULL COMMENT '是否叶子节点（1是，0不是）',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES ('1749431870600404994', NULL, '/system', 'system', NULL, NULL, '系统管理', 'SettingOutlined', 'M', 0, 2, 1, 0, NULL, NULL, 'admin4', '2026-10-22 14:02:19');
INSERT INTO `sys_menu` VALUES ('1749432380376113154', '1749431870600404994', '/system/role', 'role', 'system/role/RoleList', 'system:role:list', '角色管理', 'UserSwitchOutlined', 'C', 1, 3, 1, 0, NULL, NULL, 'admin4', '2026-10-10 16:21:15');
INSERT INTO `sys_menu` VALUES ('1749432380376113155', '2113149146301628418', '/masterdata/dept', 'masterdatadept', 'masterdata/dept/DeptList', 'system:dept:list', '部门管理', 'ApartmentOutlined', 'C', 1, 4, 1, 0, NULL, NULL, 'admin4', '2026-10-22 14:05:20');
INSERT INTO `sys_menu` VALUES ('1749432617295568897', '1749431870600404994', '/system/menu', 'menu', 'system/menu/MenuList', 'system:menu:list', '菜单管理', 'MenuOutlined', 'C', 1, 6, 1, 0, NULL, NULL, 'admin', '2026-01-10 23:00:42');
INSERT INTO `sys_menu` VALUES ('1749432617295568898', '1749431870600404994', '/system/dict', 'dict', 'system/dict/DictList', 'system:dict:list', '字典管理', 'CopyOutlined', 'C', 1, 7, 1, 0, NULL, NULL, 'admin4', '2026-10-10 16:16:02');
INSERT INTO `sys_menu` VALUES ('1754784215470551042', NULL, '/monitor', 'monitor', NULL, NULL, '系统监控', 'FundProjectionScreenOutlined', 'M', 0, 3, 1, 0, NULL, NULL, 'admin4', '2026-10-22 14:02:16');
INSERT INTO `sys_menu` VALUES ('1754785169498550273', '1754784215470551042', '/monitor/onlineuser', 'onlineuser', 'monitor/onlineuser/OnLineUserList', 'monitor:onlineuser:list', '在线用户', 'TeamOutlined', 'C', 1, 1, 1, 0, NULL, NULL, 'admin4', '2026-10-10 16:26:01');
INSERT INTO `sys_menu` VALUES ('1754788628566224898', '1754784215470551042', '/monitor/server', 'server', 'monitor/server/ServerIndex', 'monitor:server:list', '性能监控', 'ThunderboltOutlined', 'C', 1, 2, 1, 0, NULL, NULL, 'admin4', '2026-10-10 16:28:39');
INSERT INTO `sys_menu` VALUES ('1754788962474766338', '1754784215470551042', '/monitor/loglogin', 'loglogin', 'monitor/loglogin/LogLoginList', 'monitor:log:list', '登录日志', 'FileTextOutlined', 'C', 1, 4, 1, 0, NULL, NULL, 'admin4', '2026-10-10 16:27:17');
INSERT INTO `sys_menu` VALUES ('1755046069899927554', '1754784215470551042', '/monitor/cache', 'cache', 'monitor/cache/CacheIndex', 'monitor:cache:list', '缓存监控', 'HddOutlined', 'C', 1, 3, 1, 0, NULL, NULL, 'admin4', '2026-10-10 16:29:05');
INSERT INTO `sys_menu` VALUES ('1755046069899927566', '2113139662246281218', '/generator/generator', 'generatorgenerator', 'generator/gentable/GenTableList', 'tool:generator:Index', '代码生成', 'CodeOutlined', 'C', 1, 3, 1, 0, NULL, NULL, 'admin4', '2026-10-22 13:42:07');
INSERT INTO `sys_menu` VALUES ('1980379819136446465', NULL, '/notice', 'notice', '', '', '通知公告', 'SoundOutlined', 'M', 0, 9, 1, 0, 'admin', '2025-10-21 05:05:17', 'admin4', '2026-10-25 15:18:29');
INSERT INTO `sys_menu` VALUES ('1980380019431239681', '1980379819136446465', '/notice/issue', 'noticeissue', 'system/notice/Issue', 'system:notice:Issue', '发布通知', 'SoundOutlined', 'C', 1, 1, 1, 0, 'admin', '2025-10-21 05:06:05', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1980380162117267458', '1980379819136446465', '/notice/manage', 'noticemanage', 'system/notice/ManageList', 'system:notice:Manage', '通知管理', 'SoundOutlined', 'C', 1, 2, 1, 0, 'admin', '2025-10-21 05:06:39', 'admin4', '2026-10-10 16:30:04');
INSERT INTO `sys_menu` VALUES ('1980380293801635841', '1980379819136446465', '/notice/list', 'noticelist', 'system/notice/NoticeList', 'system:notice:Index', '通知列表', 'SoundOutlined', 'C', 1, 3, 1, 0, 'admin', '2025-10-21 05:07:10', 'admin4', '2026-10-10 16:18:42');
INSERT INTO `sys_menu` VALUES ('2010005313825574914', '2113149146301628418', '/masterdata/post', 'masterdatapost', 'masterdata/post/PostList', '', '岗位管理', 'ContactsOutlined', NULL, 1, 5, 1, 0, 'admin', '2026-01-10 23:06:26', 'admin4', '2026-10-22 14:05:29');
INSERT INTO `sys_menu` VALUES ('2010005751476031490', '1749431870600404994', '/system/config', 'config', 'system/config/ConfigList', '', '参数配置', 'SettingOutlined', NULL, 1, 8, 1, 0, 'admin', '2026-01-10 23:08:10', 'admin4', '2026-10-10 16:13:37');
INSERT INTO `sys_menu` VALUES ('2018684546272215041', '1754784215470551042', '/monitor/logoperation', 'logoperation', 'monitor/logoperation/LogOperationList', '', '操作日志', 'FileTextOutlined', NULL, 1, 5, 1, 0, 'admin', '2026-02-03 21:54:36', 'admin4', '2026-10-10 16:27:51');
INSERT INTO `sys_menu` VALUES ('2026101704000000001', '1749431870600404994', '/system/account', 'account', 'system/account/AccountList', 'system:account:list', '账号管理', 'UserOutlined', 'C', 1, 2, 1, 0, 'admin4', '2026-10-17 04:02:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026101704000000002', '2113149146301628418', '/masterdata/person', 'masterdataperson', 'masterdata/person/PersonList', 'system:person:list', '人员管理', 'IdcardOutlined', 'C', 1, 3, 1, 0, 'admin4', '2026-10-17 04:02:00', 'admin4', '2026-10-22 14:05:11');
INSERT INTO `sys_menu` VALUES ('2026102204000000001', NULL, '/report', 'report', NULL, NULL, '报表管理', 'BarChartOutlined', 'M', 0, 6, 1, 0, NULL, NULL, 'admin4', '2026-10-22 14:02:06');
INSERT INTO `sys_menu` VALUES ('2026102204000000002', '2026102204000000001', '/report/list', 'reportlist', 'report/ReportList', 'report:list', '报表列表', 'TableOutlined', 'C', 1, 2, 1, 0, 'admin', '2026-10-22 13:23:18', 'admin4', '2026-10-22 13:29:36');
INSERT INTO `sys_menu` VALUES ('2026102204000000010', '2026102204000000001', '/report/template', 'reporttemplate', 'report/template/TemplateList', 'report:template:list', '模板设计', 'HighlightOutlined', 'C', 1, 1, 1, 0, 'admin', '2026-10-22 13:23:18', 'admin4', '2026-10-22 13:29:12');
INSERT INTO `sys_menu` VALUES ('2026102304000000001', NULL, '/todo', 'todo', 'system/todo/TodoList', 'system:todo:list', '待办事项', 'CarryOutOutlined', 'C', 1, 8, 1, 0, 'admin4', '2026-10-23 16:45:00', 'admin4', '2026-10-24 17:57:06');
INSERT INTO `sys_menu` VALUES ('2026102304000000101', NULL, '/businessprocess', 'businessprocess', NULL, NULL, '流程审批', 'PartitionOutlined', 'M', 0, 5, 1, 0, 'admin', '2026-10-23 11:00:00', 'admin4', '2026-10-24 17:56:46');
INSERT INTO `sys_menu` VALUES ('2026102304000000102', '2026102304000000101', '/businessprocess/definition', 'businessprocessdefinition', 'businessprocess/definition/DefinitionList', 'businessprocess:definition:list', '流程配置', 'NodeIndexOutlined', 'C', 1, 2, 1, 0, 'admin', '2026-10-23 11:00:00', 'admin4', '2026-11-01 22:06:59');
INSERT INTO `sys_menu` VALUES ('2026102304000000103', '2026102304000000101', '/businessprocess/apply', 'businessprocessapply', 'businessprocess/apply/ApplyList', 'businessprocess:apply:list', '发起申请', 'SendOutlined', 'C', 1, 3, 1, 0, 'admin', '2026-10-23 11:00:00', 'admin4', '2026-11-01 22:06:53');
INSERT INTO `sys_menu` VALUES ('2026102304000000104', '2026102304000000101', '/businessprocess/todo', 'businessprocesstodo', 'businessprocess/todo/TodoList', 'businessprocess:task:todo', '待我审批', 'AuditOutlined', 'C', 1, 4, 1, 0, 'admin', '2026-10-23 11:00:00', 'admin4', '2026-11-01 22:06:49');
INSERT INTO `sys_menu` VALUES ('2026102304000000105', '2026102304000000101', '/businessprocess/mine', 'businessprocessmine', 'businessprocess/mine/MineList', 'businessprocess:instance:mine', '我的申请', 'FileDoneOutlined', 'C', 1, 5, 1, 0, 'admin', '2026-10-23 11:00:00', 'admin4', '2026-11-01 22:06:46');
INSERT INTO `sys_menu` VALUES ('2026102304000000106', '2026102304000000101', '/businessprocess/done', 'businessprocessdone', 'businessprocess/done/DoneList', 'businessprocess:task:done', '我已审批', 'CheckSquareOutlined', 'C', 1, 6, 1, 0, 'admin', '2026-10-23 11:00:00', 'admin4', '2026-11-01 22:06:42');
INSERT INTO `sys_menu` VALUES ('2026102504000000101', NULL, '/filecenter', 'filecenter', NULL, NULL, '文件中心', 'FolderOpenOutlined', 'M', 0, 4, 1, 0, NULL, NULL, 'admin4', '2026-10-25 15:17:53');
INSERT INTO `sys_menu` VALUES ('2026102504000000102', '2026102504000000101', '/filecenter/my', 'filecentermy', 'filemanage/my/MyFileList', 'filemanage:file:my', '我的文件', 'FolderOutlined', 'C', 1, 1, 1, 0, 'admin4', '2026-10-25 09:30:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102504000000104', '2026102504000000101', '/filecenter/recycle', 'filecenterrecycle', 'filemanage/recycle/RecycleList', 'filemanage:file:recycle', '回收站', 'DeleteOutlined', 'C', 1, 3, 1, 0, 'admin4', '2026-10-25 10:30:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102504000000105', '2026102504000000101', '/filecenter/quota', 'filecenterquota', 'filemanage/quota/QuotaList', 'filemanage:quota:list', '配额管理', 'PieChartOutlined', 'C', 1, 4, 1, 0, 'admin4', '2026-10-25 10:30:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102515000000101', NULL, '/onlinedoc', 'onlinedoc', NULL, NULL, '在线文档', 'BookOutlined', 'M', 0, 5, 1, 0, NULL, NULL, 'admin4', '2026-10-25 15:05:00');
INSERT INTO `sys_menu` VALUES ('2026102515000000102', '2026102515000000101', '/onlinedoc/mindmap', 'onlinedocmindmap', 'online/mindmap/MindMapList', 'online:mindmap:list', '思维导图', 'PartitionOutlined', 'C', 1, 1, 1, 0, 'admin4', '2026-10-25 15:05:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102515000000103', '2026102515000000101', '/onlinedoc/diagram', 'onlinedocdiagram', 'online/diagram/DiagramList', 'online:diagram:list', '流程图', 'ApartmentOutlined', 'C', 1, 2, 1, 0, 'admin4', '2026-10-25 15:05:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102515000000104', '2026102515000000101', '/onlinedoc/doc', 'onlinedocdoc', 'online/doc/DocList', 'online:doc:list', '协同文档', 'FileTextOutlined', 'C', 1, 3, 1, 0, 'admin4', '2026-10-28 05:00:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102516000000101', NULL, '/openapi', 'api', NULL, NULL, '接口服务', 'ApiOutlined', 'M', 0, 6, 1, 0, NULL, NULL, 'admin4', '2026-10-25 16:20:00');
INSERT INTO `sys_menu` VALUES ('2026102516000000102', '2026102516000000101', '/openapi/api', 'apiapi', 'api/designer/ApiList', 'api:api:list', '接口管理', 'LinkOutlined', 'C', 1, 1, 1, 0, 'admin4', '2026-10-25 16:20:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102516000000103', '2026102516000000101', '/openapi/data', 'apidata', 'api/data/DataList', 'api:data:list', '采集数据', 'DatabaseOutlined', 'C', 1, 2, 1, 0, 'admin4', '2026-10-25 16:20:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102804000000101', '2026102304000000101', '/businessprocess/form', 'businessprocessform', 'businessprocess/form/FormList', 'businessprocess:form:list', '在线表单', 'FormOutlined', 'C', 1, 1, 1, 0, 'admin', '2026-10-28 11:00:00', 'admin4', '2026-11-01 22:06:30');
INSERT INTO `sys_menu` VALUES ('2026102807000000101', '1754784215470551042', '/monitor/logapi', 'logapi', 'monitor/logapi/LogApiList', 'monitor:logapi:list', '接口日志', 'ApiOutlined', 'C', 1, 6, 1, 0, 'admin', '2026-10-28 13:00:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102807000000102', '1754784215470551042', '/monitor/loggenerator', 'loggenerator', 'monitor/loggenerator/LogGeneratorList', 'monitor:loggenerator:list', '代码生成日志', 'CodeOutlined', 'C', 1, 7, 1, 0, 'admin', '2026-10-28 13:00:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102914000000001', NULL, '/aiknowledge', 'aiknowledge', NULL, NULL, 'AI 知识库', 'RobotOutlined', NULL, 0, 11, 1, 0, 'admin', '2026-10-29 14:06:45', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102914000000002', '2026102914000000001', '/aiknowledge/knowledge', 'aiknowledgeknowledge', 'ai/knowledge/KnowledgeList', NULL, '知识库管理', 'BookOutlined', 'C', 1, 1, 1, 0, 'admin', '2026-10-29 14:06:45', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026102914000000003', '2026102914000000001', '/aiknowledge/searchtest', 'aiknowledgesearchtest', 'ai/knowledge/SearchTest', NULL, '检索测试', 'SearchOutlined', 'C', 1, 2, 1, 0, 'admin', '2026-10-29 14:06:45', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2026110501000000001', '1749431870600404994', '/system/datasource', 'datasource', 'system/datasource/DatasourceList', 'system:datasource:list', '数据源配置', 'DatabaseOutlined', 'C', 1, 9, 1, 0, 'admin', '2026-11-05 12:00:00', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2111231618197430273', NULL, '/visualization', 'visualization', 'visualization/VisualizationDesign', 'visualization:design', '数据大屏', 'FundProjectionScreenOutlined', 'C', 1, 7, 1, 0, 'admin4', '2026-10-17 07:03:37', 'admin', '2026-10-23 16:41:17');
INSERT INTO `sys_menu` VALUES ('2112062509100000001', '2113139662246281218', '/generator/profile', 'generatorprofile', 'generator/profile/GenProfileList', '', '项目预设', 'SettingOutlined', 'C', 1, 2, 1, 0, 'admin4', '2026-10-20 09:10:00', 'admin4', '2026-10-22 13:42:54');
INSERT INTO `sys_menu` VALUES ('2113139662246281218', NULL, '/generator', 'generator', '', '', '代码生成器', 'CodeOutlined', NULL, 0, 10, 1, 0, 'admin4', '2026-10-22 13:25:30', 'admin4', '2026-10-25 15:18:32');
INSERT INTO `sys_menu` VALUES ('2113149146301628418', NULL, '/masterdata', 'masterdata', '', '', '基础数据', 'DatabaseOutlined', NULL, 0, 1, 1, 0, 'admin4', '2026-10-22 14:03:12', 'admin4', '2026-10-22 14:03:50');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '通知内容',
  `status` int NULL DEFAULT 1 COMMENT '状态(1发布2撤销)',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES ('2116899043685429249', '通知-测试001', '<p>通知</p><p><br></p><p>测试001</p>', 1, 'admin4', '2026-11-01 22:23:57', 'admin4', '2026-11-01 22:23:57');

-- ----------------------------
-- Table structure for sys_notice_status
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_status`;
CREATE TABLE `sys_notice_status`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `notice_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通知编码',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户',
  `read_status` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读(0未读1已读)',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知 阅读状态表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_notice_status
-- ----------------------------
INSERT INTO `sys_notice_status` VALUES ('2116899043714789377', '2116899043685429249', 'admin4', 0, NULL);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `order_no` int NULL DEFAULT NULL COMMENT '排序码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态（1启用，0停用）',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES ('1745438525662351362', 'common', '普通用户', '普通用户', 2, 1, 0, NULL, NULL, 'admin', '2024-04-16 22:25:46');
INSERT INTO `sys_role` VALUES ('1750881083171422209', 'admin', '管理员', '管理员', 1, 1, 0, 'admin', '2024-01-26 21:59:15', 'admin', '2024-04-16 20:20:04');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `role_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '角色ID',
  `menu_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '权限ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES ('2118426673517613057', '1750881083171422209', '2026102304000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426673517613058', '1750881083171422209', '2026102304000000102');
INSERT INTO `sys_role_menu` VALUES ('2118426673517613059', '1750881083171422209', '1754784215470551042');
INSERT INTO `sys_role_menu` VALUES ('2118426673517613060', '1750881083171422209', '1754788628566224898');
INSERT INTO `sys_role_menu` VALUES ('2118426673517613061', '1750881083171422209', '2026102304000000103');
INSERT INTO `sys_role_menu` VALUES ('2118426673517613062', '1750881083171422209', '2026102304000000104');
INSERT INTO `sys_role_menu` VALUES ('2118426673517613063', '1750881083171422209', '1749431870600404994');
INSERT INTO `sys_role_menu` VALUES ('2118426673517613064', '1750881083171422209', '1749432617295568897');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721921', '1750881083171422209', '2010005751476031490');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721922', '1750881083171422209', '2026102304000000001');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721923', '1750881083171422209', '1749432617295568898');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721924', '1750881083171422209', '2113149146301628418');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721925', '1750881083171422209', '2026101704000000002');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721926', '1750881083171422209', '2026101704000000001');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721927', '1750881083171422209', '1755046069899927554');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721928', '1750881083171422209', '2026102204000000001');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721929', '1750881083171422209', '2026102204000000010');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721930', '1750881083171422209', '2026102914000000001');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721931', '1750881083171422209', '2026102914000000003');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721932', '1750881083171422209', '1980379819136446465');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721933', '1750881083171422209', '1980380162117267458');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721934', '1750881083171422209', '1754785169498550273');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721935', '1750881083171422209', '2010005313825574914');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721936', '1750881083171422209', '2113139662246281218');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721937', '1750881083171422209', '2112062509100000001');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721938', '1750881083171422209', '2026102304000000105');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721939', '1750881083171422209', '2026102914000000002');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721940', '1750881083171422209', '2026102304000000106');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721941', '1750881083171422209', '1749432380376113155');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721942', '1750881083171422209', '1749432380376113154');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721943', '1750881083171422209', '1980380293801635841');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721944', '1750881083171422209', '2026102504000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721945', '1750881083171422209', '2026102504000000105');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721946', '1750881083171422209', '2026110501000000001');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721947', '1750881083171422209', '1754788962474766338');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721948', '1750881083171422209', '1755046069899927566');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721949', '1750881083171422209', '2026102504000000104');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721950', '1750881083171422209', '2026102804000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426673584721951', '1750881083171422209', '2026102807000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830786', '1750881083171422209', '2111231618197430273');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830787', '1750881083171422209', '2026102504000000102');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830788', '1750881083171422209', '2026102807000000102');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830789', '1750881083171422209', '2026102516000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830790', '1750881083171422209', '2026102516000000103');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830791', '1750881083171422209', '2026102516000000102');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830792', '1750881083171422209', '2026102515000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830793', '1750881083171422209', '2026102515000000104');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830794', '1750881083171422209', '2026102515000000103');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830795', '1750881083171422209', '2026102515000000102');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830796', '1750881083171422209', '2026102204000000002');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830797', '1750881083171422209', '1980380019431239681');
INSERT INTO `sys_role_menu` VALUES ('2118426673651830798', '1750881083171422209', '2018684546272215041');
INSERT INTO `sys_role_menu` VALUES ('2118426760163545089', '1745438525662351362', '1980379819136446465');
INSERT INTO `sys_role_menu` VALUES ('2118426760163545090', '1745438525662351362', '1980380293801635841');
INSERT INTO `sys_role_menu` VALUES ('2118426760163545091', '1745438525662351362', '2026102304000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653954', '1745438525662351362', '2026102304000000103');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653955', '1745438525662351362', '2026102504000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653956', '1745438525662351362', '2026102504000000104');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653957', '1745438525662351362', '2111231618197430273');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653958', '1745438525662351362', '2026102504000000102');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653959', '1745438525662351362', '2026102304000000001');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653960', '1745438525662351362', '2113149146301628418');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653961', '1745438525662351362', '2026101704000000002');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653962', '1745438525662351362', '2026102515000000101');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653963', '1745438525662351362', '2026102515000000104');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653964', '1745438525662351362', '2026102515000000103');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653965', '1745438525662351362', '2026102515000000102');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653966', '1745438525662351362', '2026102204000000001');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653967', '1745438525662351362', '2026102204000000002');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653968', '1745438525662351362', '2010005313825574914');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653969', '1745438525662351362', '2026102304000000105');
INSERT INTO `sys_role_menu` VALUES ('2118426760230653970', '1745438525662351362', '1749432380376113155');

-- ----------------------------
-- Table structure for sys_todo
-- ----------------------------
DROP TABLE IF EXISTS `sys_todo`;
CREATE TABLE `sys_todo`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属用户（sys_account.user_name）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '待办标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '待办内容',
  `priority` tinyint NOT NULL DEFAULT 2 COMMENT '优先级（1高 2中 3低）',
  `due_date` datetime NULL DEFAULT NULL COMMENT '截止时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0待办 1完成）',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_todo_user_status`(`user_name` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_todo_due_date`(`due_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统待办' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_todo
-- ----------------------------
INSERT INTO `sys_todo` VALUES ('2116895220828991489', 'admin4', '流程审批-测试001-申请摘要', '审批任务:2116895220799631362', 2, NULL, 1, '2026-11-01 22:09:17', 'admin4', '2026-11-01 22:08:45', 'admin4', '2026-11-01 22:08:45');
INSERT INTO `sys_todo` VALUES ('2116898604042678274', 'admin4', '代办-测试001', '测试下代办', 2, '2036-06-06 06:06:06', 0, NULL, 'admin4', '2026-11-01 22:22:12', 'admin4', '2026-11-01 22:22:12');

-- ----------------------------
-- Table structure for vis_dataset
-- ----------------------------
DROP TABLE IF EXISTS `vis_dataset`;
CREATE TABLE `vis_dataset`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `screen_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属大屏ID',
  `dataset_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据集编码（同一大屏内唯一，组件按它引用）',
  `dataset_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据集名称',
  `sql_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '查询 SQL（只允许单条 SELECT，服务端做安全校验）',
  `ds_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外部数据源ID（为空=系统主库；其他值=sys_datasource.id）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_vis_dataset_screen`(`screen_id` ASC) USING BTREE,
  INDEX `idx_vis_dataset_key`(`screen_id` ASC, `dataset_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '大屏数据集' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of vis_dataset
-- ----------------------------

-- ----------------------------
-- Table structure for vis_screen
-- ----------------------------
DROP TABLE IF EXISTS `vis_screen`;
CREATE TABLE `vis_screen`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `screen_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '大屏编码（展示页路由用，全库唯一）',
  `screen_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '大屏名称',
  `config_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '大屏配置 JSON（画布 + 组件布局/样式/数据映射）',
  `thumbnail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图（相对路径，列表/画廊展示用）',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态（0草稿 1已发布 2停用）',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_vis_screen_code`(`screen_code` ASC) USING BTREE,
  INDEX `idx_vis_screen_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据大屏' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of vis_screen
-- ----------------------------
INSERT INTO `vis_screen` VALUES ('2116897896685891585', 'DP_TEST001', '大屏-测试001', '{\"canvas\":{\"width\":1920,\"height\":1080,\"background\":\"#0b1020\",\"backgroundImage\":\"\",\"scaleMode\":\"fit\",\"showGrid\":true},\"widgets\":[{\"id\":\"w_mvxx24c2_n9rxt\",\"type\":\"number\",\"name\":\"数值指标\",\"x\":100,\"y\":100,\"w\":320,\"h\":150,\"zIndex\":1,\"props\":{\"title\":\"指标名称\",\"unit\":\"\",\"precision\":0,\"fontSize\":40,\"color\":\"#40a9ff\",\"titleColor\":\"#cfe3ff\",\"background\":\"rgba(23,44,86,0.55)\",\"borderColor\":\"rgba(64,169,255,0.35)\"},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[{\"name\":\"一月\",\"value\":120},{\"name\":\"二月\",\"value\":180},{\"name\":\"三月\",\"value\":150},{\"name\":\"四月\",\"value\":240}],\"mapping\":{\"x\":\"name\",\"y\":\"value\",\"name\":\"name\",\"value\":\"value\"}}},{\"id\":\"w_mvxx27nb_mgr9f\",\"type\":\"text\",\"name\":\"文本\",\"x\":596,\"y\":40,\"w\":420,\"h\":64,\"zIndex\":2,\"props\":{\"text\":\"文本内容\",\"fontSize\":28,\"color\":\"#ffffff\",\"fontWeight\":\"bold\",\"align\":\"left\",\"background\":\"transparent\",\"letterSpacing\":0},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[],\"mapping\":{}}},{\"id\":\"w_mvxx2cub_4ubtm\",\"type\":\"bar\",\"name\":\"柱状图\",\"x\":556,\"y\":144,\"w\":620,\"h\":340,\"zIndex\":3,\"props\":{\"title\":\"柱状图\",\"color\":\"\",\"showLegend\":true},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[{\"name\":\"一月\",\"value\":120},{\"name\":\"二月\",\"value\":180},{\"name\":\"三月\",\"value\":150},{\"name\":\"四月\",\"value\":240}],\"mapping\":{\"x\":\"name\",\"y\":\"value\",\"name\":\"name\",\"value\":\"value\"}}},{\"id\":\"w_mvxx2wd5_wbu6i\",\"type\":\"line\",\"name\":\"折线图\",\"x\":1280,\"y\":104,\"w\":620,\"h\":340,\"zIndex\":4,\"props\":{\"title\":\"折线图\",\"color\":\"\",\"showLegend\":true,\"smooth\":true,\"area\":true},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[{\"name\":\"一月\",\"value\":120},{\"name\":\"二月\",\"value\":180},{\"name\":\"三月\",\"value\":150},{\"name\":\"四月\",\"value\":240}],\"mapping\":{\"x\":\"name\",\"y\":\"value\",\"name\":\"name\",\"value\":\"value\"}}},{\"id\":\"w_mvxx32lb_xugo7\",\"type\":\"number\",\"name\":\"数值指标\",\"x\":100,\"y\":292,\"w\":320,\"h\":150,\"zIndex\":5,\"props\":{\"title\":\"指标名称\",\"unit\":\"\",\"precision\":0,\"fontSize\":40,\"color\":\"#40a9ff\",\"titleColor\":\"#cfe3ff\",\"background\":\"rgba(23,44,86,0.55)\",\"borderColor\":\"rgba(64,169,255,0.35)\"},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[{\"name\":\"一月\",\"value\":120},{\"name\":\"二月\",\"value\":180},{\"name\":\"三月\",\"value\":150},{\"name\":\"四月\",\"value\":240}],\"mapping\":{\"x\":\"name\",\"y\":\"value\",\"name\":\"name\",\"value\":\"value\"}}},{\"id\":\"w_mvxx3a45_mow9e\",\"type\":\"pie\",\"name\":\"饼图\",\"x\":100,\"y\":544,\"w\":460,\"h\":340,\"zIndex\":6,\"props\":{\"title\":\"饼图\",\"color\":\"\",\"showLegend\":true,\"roseType\":false},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[{\"name\":\"一月\",\"value\":120},{\"name\":\"二月\",\"value\":180},{\"name\":\"三月\",\"value\":150},{\"name\":\"四月\",\"value\":240}],\"mapping\":{\"x\":\"name\",\"y\":\"value\",\"name\":\"name\",\"value\":\"value\"}}},{\"id\":\"w_mvxx3eel_c3jnu\",\"type\":\"table\",\"name\":\"表格\",\"x\":612,\"y\":552,\"w\":720,\"h\":360,\"zIndex\":7,\"props\":{\"title\":\"数据表格\",\"headerBg\":\"rgba(64,169,255,0.18)\",\"fontSize\":14,\"color\":\"#e6f0ff\",\"striped\":true,\"maxRows\":10},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[{\"name\":\"一月\",\"value\":120},{\"name\":\"二月\",\"value\":180},{\"name\":\"三月\",\"value\":150},{\"name\":\"四月\",\"value\":240}],\"mapping\":{\"x\":\"name\",\"y\":\"value\",\"name\":\"name\",\"value\":\"value\"}}},{\"id\":\"w_mvxx3i8u_3fgh4\",\"type\":\"image\",\"name\":\"图片\",\"x\":1436,\"y\":548,\"w\":400,\"h\":260,\"zIndex\":8,\"props\":{\"src\":\"\",\"fit\":\"cover\",\"radius\":4},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[],\"mapping\":{}}},{\"id\":\"w_mvxx3lto_i15r3\",\"type\":\"border\",\"name\":\"边框容器\",\"x\":1360,\"y\":508,\"w\":528,\"h\":368,\"zIndex\":9,\"props\":{\"title\":\"分组标题\",\"borderColor\":\"rgba(64,169,255,0.45)\",\"color\":\"#40a9ff\",\"background\":\"rgba(13,32,64,0.35)\"},\"data\":{\"mode\":\"static\",\"datasetKey\":\"\",\"staticRows\":[],\"mapping\":{}}}]}', '', 1, '2026-11-01 22:21:03', '', 0, 'admin4', '2026-11-01 22:19:23', 'admin4', '2026-11-01 22:21:03');

SET FOREIGN_KEY_CHECKS = 1;
