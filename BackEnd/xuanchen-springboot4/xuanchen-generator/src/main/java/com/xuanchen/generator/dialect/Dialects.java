package com.xuanchen.generator.dialect;

import com.xuanchen.common.exception.BusinessException;

/**
 * 方言工厂：按 sys_datasource.ds_type 取对应实现。
 *
 * @author XuanChen
 * @date 2026-10-19
 */
public final class Dialects {

    private static final DatabaseDialect MYSQL = new MySqlDialect();
    private static final DatabaseDialect POSTGRESQL = new PostgresDialect();

    private Dialects() {
    }

    /**
     * 按连接类型取方言；空值按 MySQL 处理（兼容历史数据）。
     *
     * @param connType 连接类型（mysql / mariadb / postgresql / postgres / pg）
     * @return 方言实现
     */
    public static DatabaseDialect of(String connType) {
        String key = connType == null ? "" : connType.trim().toLowerCase();
        if (key.isEmpty() || "mysql".equals(key) || "mariadb".equals(key)) {
            return MYSQL;
        }
        if ("postgresql".equals(key) || "postgres".equals(key) || "pg".equals(key)) {
            return POSTGRESQL;
        }
        throw new BusinessException(400, "暂不支持的数据库类型：" + connType + "（当前支持 mysql / postgresql）");
    }

    public static DatabaseDialect mysql() {
        return MYSQL;
    }
}
