-- ============================================================================
-- xuanchen-monolithic 第 11 批 数据库结构加固迁移（待审阅，未执行）
-- 目标库：xuanchen-monolithic   MySQL 8.0（InnoDB / utf8mb4_0900_ai_ci）
-- 内容：
--   0. 预检（重复数据 / 字面量 'NULL' / del_flag NULL）
--   1. 数据规整（幂等，可重复执行）
--   2. 唯一约束（函数索引，仅约束 del_flag=0 的在役行）
--   3. 二级索引（关系表外键列、树表父列、日志时间列等）
--   4. 雪花 ID 列 varchar(32) -> char(64)
--   5. 去除 DEFAULT 'NULL' 字面量默认值
-- 特性说明：
--   * 全部使用 MySQL 8.0.13+ 支持的函数索引（functional key index），
--     不额外新增生成列，CASE 对删除行(del_flag=1)/NULL 行求值为 NULL，
--     MySQL 唯一索引中多个 NULL 互不冲突，故“同名历史数据可多次删除、在役数据唯一”。
--   * MySQL 不支持 CREATE INDEX IF NOT EXISTS，本脚本仅可执行一次；
--     重复执行会在已存在索引处报错（不影响已完成部分）。
--   * 建议在低峰期执行；8.0 多数二级索引/列宽变更走 INSTANT/INPLACE，
--     但 char(64) 主键变更会重建表，执行前请备份并预留维护窗口。
-- 回滚：见文件末尾“回滚脚本”段。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. 预检：以下每个查询都必须返回 0 行，才能执行第 2 节的唯一索引。
--    现网库（2026-09-25 核查）全部为 0 行；在其他环境执行前必须重跑确认，
--    若有重复，先人工合并/废弃脏数据，否则第 2 节会建索引失败。
-- ----------------------------------------------------------------------------
-- SELECT user_name, COUNT(*) FROM sys_user      WHERE del_flag=0 GROUP BY user_name HAVING COUNT(*)>1;
-- SELECT email,     COUNT(*) FROM sys_user      WHERE del_flag=0 AND email IS NOT NULL AND email<>'' GROUP BY email HAVING COUNT(*)>1;
-- SELECT mobile,    COUNT(*) FROM sys_user      WHERE del_flag=0 AND mobile IS NOT NULL AND mobile<>'' GROUP BY mobile HAVING COUNT(*)>1;
-- SELECT role_code, COUNT(*) FROM sys_role      WHERE del_flag=0 GROUP BY role_code HAVING COUNT(*)>1;
-- SELECT post_code, COUNT(*) FROM sys_post      WHERE del_flag=0 GROUP BY post_code HAVING COUNT(*)>1;
-- SELECT dept_code, COUNT(*) FROM sys_dept      WHERE del_flag=0 GROUP BY dept_code HAVING COUNT(*)>1;
-- SELECT config_key,COUNT(*) FROM sys_config    WHERE del_flag=0 GROUP BY config_key HAVING COUNT(*)>1;
-- SELECT dict_code, COUNT(*) FROM sys_dict      WHERE del_flag=0 GROUP BY dict_code HAVING COUNT(*)>1;
-- SELECT dict_code, dict_item_value, COUNT(*) FROM sys_dict_item WHERE del_flag=0 GROUP BY dict_code, dict_item_value HAVING COUNT(*)>1;
-- SELECT name, COUNT(*) FROM sys_menu WHERE del_flag=0 AND name IS NOT NULL GROUP BY name HAVING COUNT(*)>1;

-- ----------------------------------------------------------------------------
-- 1. 数据规整（幂等）
-- ----------------------------------------------------------------------------
-- 1.1 del_flag 历史 NULL 归一为 0（唯一索引的 CASE 只认 del_flag=0 为在役）
UPDATE sys_user       SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE sys_role       SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE sys_menu       SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE sys_dict       SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE sys_dict_item  SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE sys_post       SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE sys_dept       SET del_flag = 0 WHERE del_flag IS NULL;
UPDATE sys_config     SET del_flag = 0 WHERE del_flag IS NULL;

-- 1.2 清理误写成字符串 'NULL' 的数据（现网核查为 0 行，保留语句防其他环境踩坑）
--     sys_user.user_name 为 NOT NULL，若真存在 'NULL' 脏行必须人工核实后改成真实用户名，
--     不能直接置 NULL（下面语句故意留注释，避免在未知数据上直接失败）：
-- UPDATE sys_user SET user_name = '<人工核实的真实用户名>' WHERE user_name = 'NULL';
UPDATE sys_role_menu   SET role_id = NULL   WHERE role_id   = 'NULL';
UPDATE sys_role_menu   SET menu_id = NULL   WHERE menu_id   = 'NULL';
UPDATE sys_user_dept   SET user_id = NULL   WHERE user_id   = 'NULL';
UPDATE sys_user_dept   SET dept_id = NULL   WHERE dept_id   = 'NULL';
UPDATE sys_user_post   SET user_id = NULL   WHERE user_id   = 'NULL';
UPDATE sys_user_post   SET post_id = NULL   WHERE post_id   = 'NULL';
UPDATE sys_dept        SET dept_name = NULL WHERE dept_name = 'NULL';
UPDATE sys_menu        SET name = NULL      WHERE name      = 'NULL';

-- ----------------------------------------------------------------------------
-- 2. 唯一约束（仅在役行）
--    说明：应用层已有查重（#7 契约 + 本批用户中心邮箱唯一校验），唯一索引是
--    并发场景的最终防线，防止两个并发请求同时通过应用层查重后写入重复数据。
-- ----------------------------------------------------------------------------
ALTER TABLE sys_user
  ADD UNIQUE KEY uk_user_active_name   ((CASE WHEN del_flag = 0 THEN user_name END)),
  -- 空串 '' 与 NULL 同样视为“未填写”不参与唯一约束，否则两个没填手机号/邮箱的用户会互相冲突
  ADD UNIQUE KEY uk_user_active_mobile ((CASE WHEN del_flag = 0 AND mobile <> '' THEN mobile END)),
  ADD UNIQUE KEY uk_user_active_email  ((CASE WHEN del_flag = 0 AND email  <> '' THEN email  END));

ALTER TABLE sys_role
  ADD UNIQUE KEY uk_role_active_code ((CASE WHEN del_flag = 0 THEN role_code END));

ALTER TABLE sys_post
  ADD UNIQUE KEY uk_post_active_code ((CASE WHEN del_flag = 0 THEN post_code END));

ALTER TABLE sys_dept
  ADD UNIQUE KEY uk_dept_active_code ((CASE WHEN del_flag = 0 THEN dept_code END));

ALTER TABLE sys_config
  ADD UNIQUE KEY uk_config_active_key ((CASE WHEN del_flag = 0 THEN config_key END));

ALTER TABLE sys_dict
  ADD UNIQUE KEY uk_dict_active_code ((CASE WHEN del_flag = 0 THEN dict_code END));

-- 字典明细在同一 dict_code 下 value 唯一；用 # 拼接复合键（# 不会出现在 code/value 中）
ALTER TABLE sys_dict_item
  ADD UNIQUE KEY uk_dict_item_active ((CASE WHEN del_flag = 0 THEN CONCAT(dict_code, '#', dict_item_value) END));

-- 路由 name 是前端动态路由的唯一标识，重名会导致路由互相覆盖
ALTER TABLE sys_menu
  ADD UNIQUE KEY uk_menu_active_name ((CASE WHEN del_flag = 0 THEN name END));

-- ----------------------------------------------------------------------------
-- 3. 二级索引
-- ----------------------------------------------------------------------------
-- 3.1 关系表：外键列（物理删除/授权保存/权限装配的高频过滤列，现状全表扫描）
ALTER TABLE sys_user_role  ADD INDEX idx_user_role_user_id (user_id),
                          ADD INDEX idx_user_role_role_id (role_id);
ALTER TABLE sys_role_menu  ADD INDEX idx_role_menu_role_id (role_id),
                          ADD INDEX idx_role_menu_menu_id (menu_id);
ALTER TABLE sys_user_dept  ADD INDEX idx_user_dept_user_id (user_id),
                          ADD INDEX idx_user_dept_dept_id (dept_id);
ALTER TABLE sys_user_post  ADD INDEX idx_user_post_user_id (user_id),
                          ADD INDEX idx_user_post_post_id (post_id);

-- 3.2 树表父列
ALTER TABLE sys_menu       ADD INDEX idx_menu_parent_id    (parent_id);
ALTER TABLE sys_dept       ADD INDEX idx_dept_parent_code  (parent_dept_code);

-- 3.3 字典明细按 dict_code 列表/翻译
ALTER TABLE sys_dict_item  ADD INDEX idx_dict_item_code    (dict_code);

-- 3.4 通知阅读状态：按通知清理、按用户未读查询
ALTER TABLE sys_notice_status ADD INDEX idx_notice_status_notice_id (notice_id),
                              ADD INDEX idx_notice_status_user_read (user_id, read_status);

-- 3.5 代码生成器关联列
ALTER TABLE gen_table        ADD INDEX idx_gen_table_database_id (database_id);
ALTER TABLE gen_table_column ADD INDEX idx_gen_table_column_table_id (table_id);

-- 3.6 日志表按用户+时间检索（管理页列表高频）
ALTER TABLE mon_log_login     ADD INDEX idx_log_login_user_time    (user_name, login_time);
ALTER TABLE mon_log_operation ADD INDEX idx_log_operation_user_time(user_name, operation_time);

-- ----------------------------------------------------------------------------
-- 4. 雪花 ID 列扩宽：varchar(32) -> char(64)
--    现状雪花 ID 为 19 位数字，varchar(32) 虽勉强容纳，但 char 定长更适合
--    纯数字定长标识，且为后续 ID 策略（如更长的分布式 ID）预留余量。
--    保持“ID 列”与“业务编码列”区分：
--      * sys_user_dept.dept_id 存的是 dept_code（如 XC001001001），保持 varchar(32)；
--      * sys_notice_status.user_id 存的是登录名 user_name（如 admin），保持 varchar(32)；
--      * create_by/update_by/log.user_name 均为人名/账号，不在本次扩宽范围。
--    每张表一条 ALTER，主键变更重建表，逐表执行便于观察耗时与锁影响。
-- ----------------------------------------------------------------------------
ALTER TABLE gen_database       MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE gen_table          MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键',
                               MODIFY COLUMN database_id CHAR(64) NULL DEFAULT NULL COMMENT '数据库ID';
ALTER TABLE gen_table_column   MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键',
                               MODIFY COLUMN table_id CHAR(64) NULL DEFAULT NULL COMMENT '表ID';
ALTER TABLE mon_log_login      MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE mon_log_operation  MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_config         MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_dept           MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '部门ID';
ALTER TABLE sys_dict           MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_dict_item      MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_menu           MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '菜单ID',
                               MODIFY COLUMN parent_id CHAR(64) NULL DEFAULT NULL COMMENT '父菜单ID';
ALTER TABLE sys_notice         MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_notice_status  MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键',
                               MODIFY COLUMN notice_id CHAR(64) NULL DEFAULT NULL COMMENT '通知编码';
ALTER TABLE sys_post           MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_role           MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_role_menu      MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键',
                               MODIFY COLUMN role_id CHAR(64) NULL DEFAULT NULL COMMENT '角色ID',
                               MODIFY COLUMN menu_id CHAR(64) NULL DEFAULT NULL COMMENT '权限ID';
ALTER TABLE sys_user           MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键';
ALTER TABLE sys_user_dept      MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键',
                               MODIFY COLUMN user_id CHAR(64) NULL DEFAULT NULL COMMENT '用户ID';
ALTER TABLE sys_user_post      MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键',
                               MODIFY COLUMN user_id CHAR(64) NULL DEFAULT NULL COMMENT '用户ID',
                               MODIFY COLUMN post_id CHAR(64) NULL DEFAULT NULL COMMENT '岗位ID';
ALTER TABLE sys_user_role      MODIFY COLUMN id CHAR(64) NOT NULL COMMENT '主键',
                               MODIFY COLUMN user_id CHAR(64) NULL DEFAULT NULL COMMENT '用户ID',
                               MODIFY COLUMN role_id CHAR(64) NULL DEFAULT NULL COMMENT '角色ID';

-- ----------------------------------------------------------------------------
-- 5. 去除 DEFAULT 'NULL' 字面量（列宽变更后再改默认值，避免重复重建）
--    这些默认值会让漏传字段时写入字符串 'NULL'，应用层 StringUtil.isEmpty 恰好
--    把字符串 'NULL' 当空值，造成“数据库非空、语义为空”的混乱数据。
-- ----------------------------------------------------------------------------
ALTER TABLE sys_dept
  MODIFY COLUMN dept_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
  NULL DEFAULT NULL COMMENT '部门名称';
ALTER TABLE sys_menu
  MODIFY COLUMN name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
  NULL DEFAULT NULL COMMENT '路由名称';
ALTER TABLE sys_user
  MODIFY COLUMN user_name VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
  NOT NULL COMMENT '用户名',
  MODIFY COLUMN password VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
  NOT NULL COMMENT '密码';
-- 关系表外键列随第 4 节已统一为 CHAR(64) NULL DEFAULT NULL，此处无需再改。

-- ============================================================================
-- 6. 待决项：sys_menu / sys_dict / sys_dict_item 的 del_flag 与 @TableLogic
-- ----------------------------------------------------------------------------
-- 现状：sys_user / sys_role 实体 delFlag 已标注 @TableLogic（removeById 逻辑删，
-- 回收站还原走 UPDATE del_flag=0）；但 sys_menu / sys_dict / sys_dict_item 实体
-- 未标注，removeById 为物理删除，del_flag 是“死列”，且本批新增的字典删除事务
-- （SysDictController#delete）也是显式物理删除主表+明细。
-- 本脚本不改应用行为。若要收敛为逻辑删除，需要的代码改动（另行评审）：
--   a. SysMenu / SysDict / SysDictItem 的 delFlag 字段加 @TableLogic；
--   b. 评审所有自定义 XML/QueryWrapper 是否都带 del_flag 条件（MP 只自动改写
--      内置方法，自定义 SQL 不生效），避免逻辑删数据仍被查出；
--   c. SysDictController#delete 改为只逻辑删主表（明细随 @TableLogic 自动逻辑删），
--      并提供字典回收站/还原能力，或明确“字典不进回收站、物理删”的产品决策；
--   d. 菜单删除需同步处理 sys_role_menu 授权残留与子菜单策略（禁用/级联）。
-- 在上述代码改动上线前，请勿仅对这三张表执行 del_flag 语义切换。
-- ============================================================================

-- ============================================================================
-- 回滚脚本（确认回滚时按节逆序执行；回滚前同样先备份）
-- ----------------------------------------------------------------------------
-- -- 5. 默认值回滚（恢复原样，一般无需回滚）
-- ALTER TABLE sys_dept MODIFY COLUMN dept_name VARCHAR(50) NULL DEFAULT 'NULL' COMMENT '部门名称';
-- ALTER TABLE sys_menu MODIFY COLUMN name VARCHAR(50) NULL DEFAULT 'NULL' COMMENT '路由名称';
-- ALTER TABLE sys_user MODIFY COLUMN user_name VARCHAR(32) NOT NULL DEFAULT 'NULL' COMMENT '用户名',
--                      MODIFY COLUMN password  VARCHAR(128) NOT NULL DEFAULT 'NULL' COMMENT '密码';
-- -- 4. 列宽回滚（若已有超过 32 位的 ID，此回滚会截断失败，属正常保护）
-- --    将第 4 节每条 MODIFY 的 CHAR(64) 换回 VARCHAR(32)，关系表外键列换回 VARCHAR(32) NULL DEFAULT 'NULL'。
-- -- 3. 删除二级索引
-- ALTER TABLE sys_user_role DROP INDEX idx_user_role_user_id, DROP INDEX idx_user_role_role_id;
-- ALTER TABLE sys_role_menu DROP INDEX idx_role_menu_role_id, DROP INDEX idx_role_menu_menu_id;
-- ALTER TABLE sys_user_dept DROP INDEX idx_user_dept_user_id, DROP INDEX idx_user_dept_dept_id;
-- ALTER TABLE sys_user_post DROP INDEX idx_user_post_user_id, DROP INDEX idx_user_post_post_id;
-- ALTER TABLE sys_menu DROP INDEX idx_menu_parent_id;
-- ALTER TABLE sys_dept DROP INDEX idx_dept_parent_code;
-- ALTER TABLE sys_dict_item DROP INDEX idx_dict_item_code;
-- ALTER TABLE sys_notice_status DROP INDEX idx_notice_status_notice_id, DROP INDEX idx_notice_status_user_read;
-- ALTER TABLE gen_table DROP INDEX idx_gen_table_database_id;
-- ALTER TABLE gen_table_column DROP INDEX idx_gen_table_column_table_id;
-- ALTER TABLE mon_log_login DROP INDEX idx_log_login_user_time;
-- ALTER TABLE mon_log_operation DROP INDEX idx_log_operation_user_time;
-- -- 2. 删除唯一索引
-- ALTER TABLE sys_user DROP INDEX uk_user_active_name, DROP INDEX uk_user_active_mobile, DROP INDEX uk_user_active_email;
-- ALTER TABLE sys_role DROP INDEX uk_role_active_code;
-- ALTER TABLE sys_post DROP INDEX uk_post_active_code;
-- ALTER TABLE sys_dept DROP INDEX uk_dept_active_code;
-- ALTER TABLE sys_config DROP INDEX uk_config_active_key;
-- ALTER TABLE sys_dict DROP INDEX uk_dict_active_code;
-- ALTER TABLE sys_dict_item DROP INDEX uk_dict_item_active;
-- ALTER TABLE sys_menu DROP INDEX uk_menu_active_name;
-- ============================================================================
