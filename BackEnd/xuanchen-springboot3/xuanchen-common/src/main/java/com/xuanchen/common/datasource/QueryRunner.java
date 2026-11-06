package com.xuanchen.common.datasource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一只读取数器：报表 / 接口管理 / 数据大屏共用。
 * <p>
 * 之前三处各写一份 PreparedStatement + ResultSet 映射，导致行数上限、超时、
 * 值归一化（BigDecimal / byte[] / Date）行为不一致；此处统一为：
 * <ul>
 *   <li>{@code setMaxRows} + 读取时行数双重截断（跨方言、SQL 无法绕过）</li>
 *   <li>{@code setQueryTimeout} 防止慢查询长期占用连接池</li>
 *   <li>统一把时间 / 大数 / 二进制 / LOB 归一化为可序列化值</li>
 * </ul>
 * 调用方负责提供已经过 SqlGuard 校验、占位符已替换为 ? 的 SQL 与有序参数列表。
 *
 * @author XuanChen
 * @date 2026-11-06
 */
public final class QueryRunner {

    private QueryRunner() {
    }

    public record QueryResult(List<String> columns, List<Map<String, Object>> rows) {
    }

    public static QueryResult run(Connection conn, String sql, List<Object> params,
                                  int maxRows, int timeoutSeconds) throws SQLException {
        int limit = Math.max(1, maxRows);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setMaxRows(limit);
            ps.setQueryTimeout(Math.max(1, timeoutSeconds));
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    Object value = params.get(i);
                    if (value == null) {
                        ps.setNull(i + 1, Types.VARCHAR);
                    } else {
                        ps.setObject(i + 1, value);
                    }
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return map(rs, limit);
            }
        }
    }

    private static QueryResult map(ResultSet rs, int limit) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next() && rows.size() < limit) {
            Map<String, Object> row = new LinkedHashMap<>(columnCount * 2);
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), normalize(rs.getObject(i)));
            }
            rows.add(row);
        }
        return new QueryResult(columns, rows);
    }

    /** JDBC 值统一可序列化：时间转 ISO 字符串、大数去尾零、二进制转 UTF-8 文本 */
    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toString();
        }
        if (value instanceof LocalDateTime || value instanceof LocalDate || value instanceof LocalTime) {
            return value.toString();
        }
        if (value instanceof Date d) {
            return new Timestamp(d.getTime()).toLocalDateTime().toString();
        }
        if (value instanceof java.util.Date d) {
            return new Timestamp(d.getTime()).toLocalDateTime().toString();
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        return value;
    }
}
