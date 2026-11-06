package com.xuanchen.generator.utils;

import com.xuanchen.common.datasource.DatasourceConnectionFactory;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.dialect.DatabaseDialect;
import com.xuanchen.generator.dialect.Dialects;
import com.xuanchen.generator.gendatabase.entity.GenDatasource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 代码生成数据库工具：统一数据源（sys_datasource）。
 * <p>
 * 连接口径与 report / api / visualization 完全一致：
 * {@link GenDatasource} 由 {@link com.xuanchen.generator.gendatabase.service.GenDatasourceResolver}
 * 从 sys_datasource 解析而来，本类只负责把连接交给
 * {@link DatasourceConnectionFactory}（按数据源 id 复用连接池，替代此前每次
 * {@code DriverManager.getConnection} 重新握手）。
 * <p>
 * DDL 由 {@link DatabaseDialect#buildCreateTable} 内部生成；表元数据走方言固定 SQL。
 *
 * @author XuanChen
 * @date 2025-09-28
 */
@Slf4j
@Component
public class DBUtil {

    private final DatasourceConnectionFactory connectionFactory;

    public DBUtil(DatasourceConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    // ==================== 连接获取 ====================

    /**
     * 按已解析的数据源配置取连接（调用方负责 close，连接归还到该数据源的池）。
     */
    public Connection getConnection(GenDatasource genDatabase) {
        if (genDatabase == null || genDatabase.getJdbcUrl() == null || genDatabase.getJdbcUrl().isBlank()) {
            throw new BusinessException(500, "数据源缺少 JDBC 连接串");
        }
        try {
            return connectionFactory.getConnection(genDatabase.getId(), toInfo(genDatabase));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "连接数据库失败：" + e.getMessage());
        }
    }

    /**
     * GenDatasource → DatasourceInfo：GenDatasourceResolver 已把 sys_datasource 的字段完整搬过来，
     * 这里只做结构转换，不再回查数据库。
     */
    private DatasourceInfo toInfo(GenDatasource genDatabase) {
        DatasourceInfo info = new DatasourceInfo();
        info.setId(genDatabase.getId());
        info.setDsName(genDatabase.getConnName());
        info.setDsType(genDatabase.getConnType());
        info.setJdbcUrl(genDatabase.getJdbcUrl());
        info.setUsername(genDatabase.getUserName());
        info.setPassword(genDatabase.getPassword());
        info.setStatus(genDatabase.getStatus());
        return info;
    }

    // ==================== URL / 方言 ====================

    /** 取完整 JDBC URL（GenDatasource 已由 GenDatasourceResolver 预取好） */
    public String buildUrl(GenDatasource genDatabase) {
        return genDatabase.getJdbcUrl();
    }

    /** 按数据源配置取方言 */
    public static DatabaseDialect dialectOf(GenDatasource genDatabase) {
        return Dialects.of(genDatabase.getConnType());
    }

    // ==================== DDL 执行 ====================

    /**
     * 执行 DDL（建表）。sqls 由 DatabaseDialect.buildCreateTable 内部生成，已校验。
     */
    public void executeDdl(GenDatasource genDatabase, String... sqls) {
        if (sqls == null || sqls.length == 0) return;
        try (Connection conn = getConnection(genDatabase);
             Statement statement = conn.createStatement()) {
            for (String sql : sqls) {
                statement.execute(sql);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (SQLException e) {
            throw new BusinessException(500, "执行建表语句失败：" + e.getMessage());
        }
    }

    /**
     * 执行 DDL（多语句版本）。
     */
    public void executeDdl(GenDatasource genDatabase, List<String> sqls) {
        if (sqls == null || sqls.isEmpty()) return;
        executeDdl(genDatabase, sqls.toArray(new String[0]));
    }

    // ==================== 元数据查询 ====================

    /** 获取数据库表列表（走方言内部固定 SQL，无需额外守卫） */
    public List<Map<String, Object>> getTableList(GenDatasource genDatabase) {
        try (Connection conn = getConnection(genDatabase)) {
            return dialectOf(genDatabase).listTables(conn, genDatabase.getDbName());
        } catch (SQLException e) {
            throw new BusinessException(500, "读取数据库表列表失败：" + e.getMessage());
        }
    }

    /** 获取数据库表字段信息（走方言内部固定 SQL，无需额外守卫） */
    public List<Map<String, Object>> getTableInfo(GenDatasource genDatabase, String tableName) {
        try (Connection conn = getConnection(genDatabase)) {
            return dialectOf(genDatabase).listColumns(conn, genDatabase.getDbName(), tableName);
        } catch (SQLException e) {
            throw new BusinessException(500, "读取数据库表字段失败：" + e.getMessage());
        }
    }
}
