package com.xuanchen.generator.utils;

import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 工具类-->数据库
 *
 * @author XuanChen
 * @date 2025-09-28
 */
public class DBUtil {

    /**
     * 主机：域名 / IPv4 / 括号包裹的 IPv6，只允许白名单字符，禁止任何 URL 结构字符与空白
     */
    private static final Pattern HOST_PATTERN = Pattern.compile("[A-Za-z0-9_.\\-:\\[\\]]{1,255}");

    /**
     * MySQL 库名：字母数字下划线、连字符、$，最长 64。显式禁止 ? & ` # / 空格等，
     * 防止库名拼接进 JDBC URL 时注入 allowLoadLocalInfile/autoDeserialize 等恶意连接属性
     */
    private static final Pattern DB_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_$\\-]{1,64}");

    /**
     * 数据库账号：字母数字及 . _ - @ $，最长 64
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_.@$\\-]{1,64}");

    /**
     * 获取外部数据源的 JDBC 连接。
     * <p>
     * 使用 JDK 原生 {@link DriverManager}，不引入连接池：代码生成场景为低频的手动一次性查询，
     * 连接随用随关即可，避免为低频功能额外引入 Druid 依赖。
     *
     * @param genDatabase 数据源配置
     * @return 已打开的连接（调用方负责关闭）
     */
    public static Connection getConnection(GenDatabase genDatabase) {
        String url = buildUrl(genDatabase);
        try {
            return DriverManager.getConnection(url, genDatabase.getUserName(), genDatabase.getPassword());
        } catch (SQLException e) {
            throw new BusinessException(500, "连接数据库失败，请检查主机、端口、库名、账号密码：" + e.getMessage());
        }
    }

    /**
     * 按数据源配置拼 JDBC URL（先校验主机/端口/库名，连接属性固定为服务端白名单）
     */
    public static String buildUrl(GenDatabase genDatabase) {
        return buildMySqlUrl(genDatabase.getHost(), genDatabase.getPort(), genDatabase.getDbName());
    }

    /**
     * 构建 MySQL JDBC URL。
     * <p>
     * host/port/dbName 必须通过白名单校验；"?" 之后的连接属性全部由本方法固定，
     * 调用方无法追加任何属性，杜绝 {@code dbName=x?allowLoadLocalInfile=true} 形式的属性注入。
     */
    public static String buildMySqlUrl(String host, String port, String dbName) {
        validateHost(host);
        validatePort(port);
        validateDbName(dbName);
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?serverTimezone=GMT%2B8&characterEncoding=utf-8&useSSL=false"
                + "&tinyInt1isBit=true&allowPublicKeyRetrieval=true";
    }

    /**
     * 保存数据源前的完整校验：主机/端口/库名/用户名均须为白名单格式，
     * 从入口阻断恶意库名/主机落库后被用于连接或代码生成。
     */
    public static void validateConnection(GenDatabase genDatabase) {
        if (genDatabase == null) {
            throw new BusinessException(400, "数据源配置不能为空");
        }
        validateHost(genDatabase.getHost());
        validatePort(genDatabase.getPort());
        validateDbName(genDatabase.getDbName());
        if (genDatabase.getUserName() == null || !USERNAME_PATTERN.matcher(genDatabase.getUserName()).matches()) {
            throw new BusinessException(400, "数据库用户名仅允许字母、数字及 . _ - @ $ 字符（最长 64 位）");
        }
    }

    private static void validateHost(String host) {
        if (host == null || !HOST_PATTERN.matcher(host).matches()) {
            throw new BusinessException(400,
                    "数据库主机仅允许域名、IPv4 或 [IPv6] 格式，禁止空白及 : / ? & # @ 等特殊字符");
        }
    }

    private static void validatePort(String port) {
        if (port == null || !port.matches("\\d{1,5}")) {
            throw new BusinessException(400, "数据库端口必须为 1-65535 的数字");
        }
        int p = Integer.parseInt(port);
        if (p < 1 || p > 65535) {
            throw new BusinessException(400, "数据库端口必须为 1-65535 的数字");
        }
    }

    private static void validateDbName(String dbName) {
        if (dbName == null || !DB_NAME_PATTERN.matcher(dbName).matches()) {
            throw new BusinessException(400,
                    "数据库名称仅允许字母、数字、下划线、连字符与 $（最长 64 位），禁止 ?、&、反引号等特殊字符");
        }
    }

    /**
     * 获取数据库表列表
     *
     * @param genDatabase 数据源配置
     * @return 表名 + 表注释
     */
    public static List<Map<String, Object>> getTableList(GenDatabase genDatabase) {
        List<Map<String, Object>> list = new ArrayList<>();
        String strSql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME";
        try (Connection conn = getConnection(genDatabase);
             PreparedStatement ps = conn.prepareStatement(strSql)) {
            ps.setString(1, genDatabase.getDbName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("tableName", rs.getString("TABLE_NAME"));//表名
                    map.put("tableComment", rs.getString("TABLE_COMMENT"));//表注释
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new BusinessException(500, "读取数据库表列表失败：" + e.getMessage());
        }
        return list;
    }

    /**
     * 获取数据库表字段信息
     *
     * @param genDatabase 数据源配置
     * @param tableName   物理表名
     * @return 字段元数据列表
     */
    public static List<Map<String, Object>> getTableInfo(GenDatabase genDatabase, String tableName) {
        List<Map<String, Object>> list = new ArrayList<>();
        String strSql = "SELECT COLUMN_NAME,COLUMN_COMMENT,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,NUMERIC_SCALE,"
                + "IS_NULLABLE,COLUMN_DEFAULT,COLUMN_KEY,EXTRA FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY COLUMN_KEY DESC, ORDINAL_POSITION";
        try (Connection conn = getConnection(genDatabase);
             PreparedStatement ps = conn.prepareStatement(strSql)) {
            // 参数化查询，表名/库名走占位符，杜绝外部数据源配置场景下的 SQL 拼接
            ps.setString(1, genDatabase.getDbName());
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("columnName", rs.getString("COLUMN_NAME"));//字段名
                    map.put("columnComment", rs.getString("COLUMN_COMMENT"));//字段注释
                    map.put("columnType", rs.getString("DATA_TYPE"));//字段类型
                    map.put("columnLength", rs.getString("CHARACTER_MAXIMUM_LENGTH"));//字段最大长度
                    map.put("columnScale", rs.getString("NUMERIC_SCALE"));//字段小数位长度
                    map.put("columnDefault", rs.getString("COLUMN_DEFAULT"));//字段默认值
                    map.put("columnIsPk", "PRI".equals(rs.getString("COLUMN_KEY")));//字段是否主键
                    map.put("columnIsNullable", "YES".equals(rs.getString("IS_NULLABLE")));//字段是否允许为NULL
                    map.put("extra", rs.getString("EXTRA"));//扩展信息
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new BusinessException(500, "读取数据库表字段失败：" + e.getMessage());
        }
        return list;
    }
}
