/*
 Navicat Premium Dump SQL

 Source Server         : 本地_MySQL
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : xuanchen-monolithic

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 26/01/2026 23:34:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
INSERT INTO `sys_config` VALUES ('2015795773986979841', '验证码开关', '是否启用验证码true启用false关闭', 'captchaEnabled', 'true', 1, 1, 1, 0, 'admin', '2026-01-26 22:35:39', 'admin', '2026-01-26 23:30:33');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES ('1995995435964747777', 'XC001', NULL, '萱晨集团', NULL, 1, 1, 0, 'admin', '2025-12-03 07:16:10', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995478822146050', 'XC001001', 'XC001', '萱晨软件', NULL, 1, 1, 0, 'admin', '2025-12-03 07:16:21', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995543003385857', 'XC001002', 'XC001', '萱晨硬件', NULL, 2, 1, 0, 'admin', '2025-12-03 07:16:36', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995637148733441', 'XC001001001', 'XC001001', '销售部', NULL, 1, 1, 0, 'admin', '2025-12-03 07:16:58', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995682933755905', 'XC001001002', 'XC001001', '工程部', NULL, 2, 1, 0, 'admin', '2025-12-03 07:17:09', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995738382454786', 'XC001001003', 'XC001001', '技术部', NULL, NULL, 1, 0, 'admin', '2025-12-03 07:17:23', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995777896992769', 'XC001001004', 'XC001001', '研发部', NULL, 4, 1, 0, 'admin', '2025-12-03 07:17:32', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995817906458625', 'XC001001005', 'XC001001', '宣传部', NULL, NULL, 1, 0, 'admin', '2025-12-03 07:17:41', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995900093845506', 'XC001002001', 'XC001002', '销售部', NULL, 1, 1, 0, 'admin', '2025-12-03 07:18:01', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995938803077122', 'XC001002002', 'XC001002', '工程部', NULL, 2, 1, 0, 'admin', '2025-12-03 07:18:10', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995995976459538433', 'XC001002003', 'XC001002', '技术部', NULL, 3, 1, 0, 'admin', '2025-12-03 07:18:19', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995996035045576705', 'XC001002004', 'XC001002', '研发部', NULL, 4, 1, 0, 'admin', '2025-12-03 07:18:33', NULL, NULL);
INSERT INTO `sys_dept` VALUES ('1995996075965206530', 'XC001002005', 'XC001002', '宣传部', NULL, 5, 1, 0, 'admin', '2025-12-03 07:18:43', NULL, NULL);

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
INSERT INTO `sys_dict` VALUES ('2012158527658344450', 'config_type', '参数类型', 2, 1, 0, 'admin', '2026-01-16 21:42:32', NULL, NULL);
INSERT INTO `sys_dict` VALUES ('2015181385600995330', 'user_status', '账号状态', 3, 1, 0, 'admin', '2026-01-25 05:54:17', NULL, NULL);

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
INSERT INTO `sys_dict_item` VALUES ('1988219316371537922', 'yes_no', '是', 'true', 1, 1, 0, 'admin', '2025-11-11 20:16:39', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1988219354069942274', 'yes_no', '否', 'false', 2, 1, 0, 'admin', '2025-11-11 20:16:48', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2012158582834413569', 'config_type', '系统', '1', 1, 1, 0, 'admin', '2026-01-16 21:42:45', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2012158635976245250', 'config_type', '业务', '2', 2, 1, 0, 'admin', '2026-01-16 21:42:58', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2015181432967270402', 'user_status', '正常', '1', 1, 1, 0, 'admin', '2026-01-25 05:54:29', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('2015181480673284097', 'user_status', '冻结', '2', 2, 1, 0, 'admin', '2026-01-25 05:54:40', NULL, NULL);

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
INSERT INTO `sys_menu` VALUES ('1749431870600404994', NULL, '/system', 'system', NULL, NULL, '系统管理', 'SettingOutlined', 'M', 0, 1, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1749432234737295362', '1749431870600404994', '/system/user', 'user', 'system/user/Index', 'system:user:list', '用户管理', 'TeamOutlined', 'C', 1, 2, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1749432380376113154', '1749431870600404994', '/system/role', 'role', 'system/role/Index', 'system:role:list', '角色管理', 'UserSwitchOutlined', 'C', 1, 3, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1749432380376113155', '1749431870600404994', '/system/dept', 'dept', 'system/dept/Index', 'system:dept:list', '部门管理', 'ApartmentOutlined', 'C', 1, 4, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1749432617295568897', '1749431870600404994', '/system/menu', 'menu', 'system/menu/Index', 'system:menu:list', '菜单管理', 'MenuOutlined', 'C', 1, 6, 1, 0, NULL, NULL, 'admin', '2026-01-10 23:00:42');
INSERT INTO `sys_menu` VALUES ('1749432617295568898', '1749431870600404994', '/system/dict', 'dict', 'system/dict/Index', 'system:dict:list', '字典管理', 'CopyOutlined', 'C', 1, 7, 1, 0, NULL, NULL, 'admin', '2026-01-10 23:00:36');
INSERT INTO `sys_menu` VALUES ('1754784215470551042', NULL, '/monitor', 'monitor', NULL, NULL, '系统监控', 'FundProjectionScreenOutlined', 'M', 0, 2, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1754785169498550273', '1754784215470551042', '/monitor/onlineuser', 'onlineuser', 'monitor/onlineuser/Index', 'monitor:onlineuser:list', '在线用户', 'TeamOutlined', 'C', 1, 1, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1754788628566224898', '1754784215470551042', '/monitor/server', 'server', 'monitor/server/Index', 'monitor:server:list', '性能监控', 'ThunderboltOutlined', 'C', 1, 2, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1754788962474766338', '1754784215470551042', '/monitor/log', 'log', 'monitor/log/Index', 'monitor:log:list', '日志管理', 'FileTextOutlined', 'C', 1, 3, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1755046069899927554', '1754784215470551042', '/monitor/cache', 'cache', 'monitor/cache/Index', 'monitor:cache:list', '缓存监控', 'HddOutlined', 'C', 1, 4, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1755046069899927555', NULL, '/tool', 'tool', NULL, NULL, '系统工具', 'ToolOutlined', 'M', 0, 3, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1755046069899927566', '1755046069899927555', '/tool/generator', 'generator', 'tool/generator/Index', 'tool:generator:Index', '代码生成器', 'CodeOutlined', 'C', 1, 1, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1980379819136446465', NULL, '/notice', 'notice', '', '', '通知公告', 'SoundOutlined', 'M', 0, 10, 1, 0, 'admin', '2025-10-21 05:05:17', 'admin', '2025-10-21 05:06:05');
INSERT INTO `sys_menu` VALUES ('1980380019431239681', '1980379819136446465', '/notice/issue', 'noticeissue', 'system/notice/Issue', 'system:notice:Issue', '发布通知', 'SoundOutlined', 'C', 1, 1, 1, 0, 'admin', '2025-10-21 05:06:05', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1980380162117267458', '1980379819136446465', '/notice/manage', 'noticemanage', 'system/notice/Manage', 'system:notice:Manage', '通知管理', 'SoundOutlined', 'C', 1, 2, 1, 0, 'admin', '2025-10-21 05:06:39', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('1980380293801635841', '1980379819136446465', '/notice/list', 'noticelist', 'system/notice/Index', 'system:notice:Index', '通知列表', 'SoundOutlined', 'C', 1, 3, 1, 0, 'admin', '2025-10-21 05:07:10', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2010005313825574914', '1749431870600404994', '/system/post', 'post', 'system/post/Index', '', '岗位管理', 'ContactsOutlined', NULL, 1, 5, 1, 0, 'admin', '2026-01-10 23:06:26', NULL, NULL);
INSERT INTO `sys_menu` VALUES ('2010005751476031490', '1749431870600404994', '/system/config', 'config', 'system/config/Index', '', '参数配置', 'SettingOutlined', NULL, 1, 8, 1, 0, 'admin', '2026-01-10 23:08:10', 'admin', '2026-01-11 22:37:27');

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
INSERT INTO `sys_notice` VALUES ('1980398692455854082', '通知 2025102101', '<p>通知 2025102101          通知 2025102101             通知 2025102101</p><p><br></p><p>通知 2025102101          通知 2025102101             通知 2025102101</p><p><br></p><p>通知 2025102101          通知 2025102101             通知 2025102101</p>', 1, 'admin', '2025-10-21 06:20:17', NULL, NULL);
INSERT INTO `sys_notice` VALUES ('1980398877042978818', '通知 2025102102', '<p>通知 2025102102          通知 2025102102             通知 2025102102</p><p><br></p><p><br></p><p>               通知 2025102102          通知 2025102102             通知 2025102102</p><p><br></p><p><br></p><p><br></p><p>通知 2025102102          通知 2025102102             通知 2025102102</p>', 1, 'admin', '2025-10-21 06:21:01', NULL, NULL);

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
INSERT INTO `sys_notice_status` VALUES ('1980398692497797122', '1980398692455854082', 'admin', 1, '2025-10-29 14:46:58');
INSERT INTO `sys_notice_status` VALUES ('1980398692556517377', '1980398692455854082', 'test02', 0, NULL);
INSERT INTO `sys_notice_status` VALUES ('1980398877110087681', '1980398877042978818', 'admin', 0, NULL);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES ('2010855310054043650', 'P0001', '总裁', '', 1, 1, 0, 'admin', '2026-01-13 07:24:00', NULL, NULL);
INSERT INTO `sys_post` VALUES ('2010976013071847425', 'P0002', '总经理', '', 2, 1, 0, 'admin', '2026-01-13 15:23:38', NULL, NULL);
INSERT INTO `sys_post` VALUES ('2010976279695364098', 'P0003', '副总经理', '', 3, 1, 0, 'admin', '2026-01-13 15:24:42', NULL, NULL);
INSERT INTO `sys_post` VALUES ('2010976815773552641', 'P0004', '财务', '', 4, 1, 0, 'admin', '2026-01-13 15:26:50', NULL, NULL);

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
INSERT INTO `sys_role_menu` VALUES ('1980380583716122626', '1745438525662351362', '1980379819136446465');
INSERT INTO `sys_role_menu` VALUES ('1980380583716122627', '1745438525662351362', '1980380293801635841');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653954', '1750881083171422209', '1749431870600404994');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653955', '1750881083171422209', '1749432234737295362');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653956', '1750881083171422209', '1749432380376113154');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653957', '1750881083171422209', '1749432380376113155');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653958', '1750881083171422209', '1749432617295568897');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653959', '1750881083171422209', '1749432617295568898');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653960', '1750881083171422209', '1754784215470551042');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653961', '1750881083171422209', '1754785169498550273');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653962', '1750881083171422209', '1754788628566224898');
INSERT INTO `sys_role_menu` VALUES ('2010006229798653963', '1750881083171422209', '1754788962474766338');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014081', '1750881083171422209', '1755046069899927554');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014082', '1750881083171422209', '1755046069899927555');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014083', '1750881083171422209', '1755046069899927566');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014084', '1750881083171422209', '1980379819136446465');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014085', '1750881083171422209', '1980380293801635841');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014086', '1750881083171422209', '1980380162117267458');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014087', '1750881083171422209', '1980380019431239681');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014088', '1750881083171422209', '1754784215470551042');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014089', '1750881083171422209', '1755046069899927555');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014090', '1750881083171422209', '1980379819136446465');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014091', '1750881083171422209', '2010005313825574914');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014092', '1750881083171422209', '2010005751476031490');
INSERT INTO `sys_role_menu` VALUES ('2010006229828014093', '1750881083171422209', '1749431870600404994');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NULL' COMMENT '用户名',
  `nick_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NULL' COMMENT '密码',
  `mobile` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '账号状态（1正常2冻结）',
  `salt` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '盐值',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态（0正常，1已删除）',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('1757768950749499393', 'admin', '管理员', '0c633818ae54281006d5ef2ee7559d6af1df38d4fe93aea77c733adc9a817df2d3149ce6c7e053e97d67de3d028ec61c921e4b4b6b66e3b787e193bb0a6e5d39', '18931540689', 'admin@xuanchen.com', 'avatar\\pic1_1769293937728.png', 1, 'hBOpLQrn', 0, 'super', '2024-02-14 22:09:10', 'admin', '2026-01-25 06:32:19');
INSERT INTO `sys_user` VALUES ('1757769284339273730', 'test01', '测试01', '77a399ac55ae7b8400f27ef6c7d936d623bf54137a6c356cd96d97d14895dcae756e60f1cfd70bc3b2069800f206950ef1ef13169898df979b034217af528acb', '18931541689', 'test01@xuanchen.com', 'avatar\\2025-04-21_221928_1745245996042.png', 1, 'RAtMIWos', 0, 'admin', '2024-02-14 22:10:30', 'admin', '2026-01-06 07:05:51');
INSERT INTO `sys_user` VALUES ('1757769358909804546', 'test02', '测试02', '9e83250f13f5ad1d60c468602bdc399237cfa5462d1f431fca9726a888f14fb086ab3abba7197b420397f23cfa87f072f57afb3b77d8b343e0c4a434861d2b52', '18931542689', 'test02@xuanchen.com', 'avatar\\2025-04-21_221928_1745246499366.png', 1, 'icXXjqXS', 0, 'admin', '2024-02-14 22:10:48', 'admin', '2025-11-11 18:33:15');
INSERT INTO `sys_user` VALUES ('1772395839434694658', 'test03', '测试03', 'f867ff114a35884dce6879007c0d6f80ed5215cd584bc21aa94425dc5795216d7d2b6fc7110abd503eba305eeed65bbd165093ee0742346b6a20170ae8f8c2d5', '18931543689', 'test03@xuanchen.com', 'avatar\\2025-04-21_221928_1745246712100.png', 1, 'WV!cdspi', 0, NULL, '2024-03-26 06:51:12', 'admin', '2025-11-11 18:33:22');
INSERT INTO `sys_user` VALUES ('1772396269187276802', 'test04', '测试04', '8adc7121a7680e80c2bf7220d04e025ecdf5fca6110f603c29c4e94cf2873d161d5cf22b14ac30d0d7d58cea9000e7bd9a595f09b2e67514579932cf6d0b8bd8', '18931544689', 'test04@xuanchen.com', '', 1, 'fucgEGEe', 0, NULL, '2024-03-26 06:52:55', 'admin', '2026-01-06 07:06:13');
INSERT INTO `sys_user` VALUES ('1779508954504761345', 'test05', '测试05', 'dfccc65fb09352a3f7fb7e971d96aaa9feafbc38fd5d61815ef2dc8c721743b009e4bcdc9c19cef1805a905157ee8fb66d588334803d93519608a9117631aacd', '18931545689', 'test05@xuanchen.com', '', 1, 'cFy&xQYO', 0, 'admin', '2024-04-14 21:56:11', 'admin', '2026-01-06 07:06:28');
INSERT INTO `sys_user` VALUES ('1779511801023696897', 'test06', '测试06', 'd67ed4a624a53499d0bfa739aaf4e59071314a9cf1dd9cf59a17577a6d80d8d5f90c2dab9e3819b81321532261fea439d0869869961c5a0e980c98941389cc61', '18931546689', 'test06@xuanchen.com', '', 1, 'iRbcpUG%', 0, 'admin', '2024-04-14 22:07:30', 'admin', '2026-01-25 06:01:48');

-- ----------------------------
-- Table structure for sys_user_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_dept`;
CREATE TABLE `sys_user_dept`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '用户ID',
  `dept_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '部门ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_dept
-- ----------------------------
INSERT INTO `sys_user_dept` VALUES ('2008314025419014145', '1757769284339273730', 'XC001001001');
INSERT INTO `sys_user_dept` VALUES ('2008314025419014146', '1757769284339273730', 'XC001001002');
INSERT INTO `sys_user_dept` VALUES ('2008314119774076929', '1772396269187276802', 'XC001002');
INSERT INTO `sys_user_dept` VALUES ('2008314179647766530', '1779508954504761345', 'XC001002003');
INSERT INTO `sys_user_dept` VALUES ('2015183277303402498', '1779511801023696897', 'XC001002005');
INSERT INTO `sys_user_dept` VALUES ('2015190956537221122', '1757768950749499393', 'XC001');

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '用户ID',
  `post_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '岗位ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户岗位关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES ('2015183277370511361', '1779511801023696897', '2010976815773552641');
INSERT INTO `sys_user_post` VALUES ('2015190956537221123', '1757768950749499393', '2010855310054043650');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '用户ID',
  `role_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NULL' COMMENT '角色ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES ('1988193296251699201', '1757769358909804546', '1745438525662351362');
INSERT INTO `sys_user_role` VALUES ('1988193324286427138', '1772395839434694658', '1745438525662351362');
INSERT INTO `sys_user_role` VALUES ('2008314025351905282', '1757769284339273730', '1745438525662351362');
INSERT INTO `sys_user_role` VALUES ('2008314119623081985', '1772396269187276802', '1745438525662351362');
INSERT INTO `sys_user_role` VALUES ('2008314179647766529', '1779508954504761345', '1745438525662351362');
INSERT INTO `sys_user_role` VALUES ('2015183277303402497', '1779511801023696897', '1745438525662351362');
INSERT INTO `sys_user_role` VALUES ('2015190956470112257', '1757768950749499393', '1750881083171422209');

SET FOREIGN_KEY_CHECKS = 1;
