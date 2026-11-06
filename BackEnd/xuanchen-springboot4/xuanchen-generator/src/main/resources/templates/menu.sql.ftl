-- =====================================================================
-- 菜单权限 SQL --> ${tableComment}
-- 使用说明：
--   1. 菜单的 parent_id 取自「表配置 - 上级菜单ID」，为 '0' 时表示挂到根菜单；
--   2. 图标 / 排序码可按需调整；
--   3. 执行后请在"菜单管理"中确认层级，并给相关角色分配按钮权限。
-- 生成时间：${.now?string("yyyy-MM-dd HH:mm:ss")}
-- =====================================================================

-- 菜单（C 类型）
SET @menu_id = REPLACE(UUID(), '-', '');
INSERT INTO sys_menu (id, parent_id, path, name, component, perms, title, icon, menu_type, is_leaf, order_no, status, del_flag, create_by, create_time)
VALUES (@menu_id, '${parentMenuId}', '/${vueDir}', '${entityPath}', '${vueDir}/Index', '${permsBase}:list', '${tableComment?replace("'", "''")}', 'AppstoreOutlined', 'C', 1, 99, 1, 0, 'admin', NOW());

-- 按钮权限（F 类型）
SET @btn_add = REPLACE(UUID(), '-', '');
INSERT INTO sys_menu (id, parent_id, path, name, component, perms, title, icon, menu_type, is_leaf, order_no, status, del_flag, create_by, create_time)
VALUES (@btn_add, @menu_id, NULL, NULL, NULL, '${permsBase}:add', '添加', NULL, 'F', 1, 1, 1, 0, 'admin', NOW());

SET @btn_edit = REPLACE(UUID(), '-', '');
INSERT INTO sys_menu (id, parent_id, path, name, component, perms, title, icon, menu_type, is_leaf, order_no, status, del_flag, create_by, create_time)
VALUES (@btn_edit, @menu_id, NULL, NULL, NULL, '${permsBase}:edit', '编辑', NULL, 'F', 1, 2, 1, 0, 'admin', NOW());

SET @btn_delete = REPLACE(UUID(), '-', '');
INSERT INTO sys_menu (id, parent_id, path, name, component, perms, title, icon, menu_type, is_leaf, order_no, status, del_flag, create_by, create_time)
VALUES (@btn_delete, @menu_id, NULL, NULL, NULL, '${permsBase}:delete', '删除', NULL, 'F', 1, 3, 1, 0, 'admin', NOW());

SET @btn_delete_batch = REPLACE(UUID(), '-', '');
INSERT INTO sys_menu (id, parent_id, path, name, component, perms, title, icon, menu_type, is_leaf, order_no, status, del_flag, create_by, create_time)
VALUES (@btn_delete_batch, @menu_id, NULL, NULL, NULL, '${permsBase}:deleteBatch', '批量删除', NULL, 'F', 1, 4, 1, 0, 'admin', NOW());

SET @btn_export = REPLACE(UUID(), '-', '');
INSERT INTO sys_menu (id, parent_id, path, name, component, perms, title, icon, menu_type, is_leaf, order_no, status, del_flag, create_by, create_time)
VALUES (@btn_export, @menu_id, NULL, NULL, NULL, '${permsBase}:exportExcel', '导出', NULL, 'F', 1, 5, 1, 0, 'admin', NOW());

SET @btn_import = REPLACE(UUID(), '-', '');
INSERT INTO sys_menu (id, parent_id, path, name, component, perms, title, icon, menu_type, is_leaf, order_no, status, del_flag, create_by, create_time)
VALUES (@btn_import, @menu_id, NULL, NULL, NULL, '${permsBase}:importExcel', '导入', NULL, 'F', 1, 6, 1, 0, 'admin', NOW());
