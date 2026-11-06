package com.xuanchen.generator.dialect;

import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.utils.TableDdlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 方言实现。
 *
 * @author XuanChen
 * @date 2026-10-19
 */
public class MySqlDialect implements DatabaseDialect {

    @Override
    public String key() {
        return "mysql";
    }

    @Override
    public String buildUrl(String host, String port, String dbName) {
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?serverTimezone=GMT%2B8&characterEncoding=utf-8&useSSL=false"
                + "&tinyInt1isBit=true&allowPublicKeyRetrieval=true";
    }

    @Override
    public List<Map<String, Object>> listTables(Connection conn, String dbName) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("tableName", rs.getString("TABLE_NAME"));
                    map.put("tableComment", rs.getString("TABLE_COMMENT"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> listColumns(Connection conn, String dbName, String tableName) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME,COLUMN_COMMENT,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,NUMERIC_SCALE,"
                + "IS_NULLABLE,COLUMN_DEFAULT,COLUMN_KEY,EXTRA FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY COLUMN_KEY DESC, ORDINAL_POSITION";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dbName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("columnName", rs.getString("COLUMN_NAME"));
                    map.put("columnComment", rs.getString("COLUMN_COMMENT"));
                    map.put("columnType", rs.getString("DATA_TYPE"));
                    map.put("columnLength", rs.getString("CHARACTER_MAXIMUM_LENGTH"));
                    map.put("columnScale", rs.getString("NUMERIC_SCALE"));
                    map.put("columnDefault", rs.getString("COLUMN_DEFAULT"));
                    map.put("columnIsPk", "PRI".equals(rs.getString("COLUMN_KEY")));
                    map.put("columnIsNullable", "YES".equals(rs.getString("IS_NULLABLE")));
                    map.put("extra", rs.getString("EXTRA"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    @Override
    public List<String> buildCreateTable(GenTable table, List<GenTableColumn> columns) {
        return List.of(TableDdlBuilder.build(table, columns));
    }
}
