package com.xuanchen.common.utils;

import com.xuanchen.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 安全守卫。
 * <p>
 * 数据集 SQL 只允许单条只读 SELECT：剥离注释后校验起始关键字与危险关键字黑名单。
 * <b>行数上限不再靠拼接 LIMIT</b>——sys_datasource 支持 Oracle / SQL Server / DM / Kingbase，
 * 这些方言并不支持 MySQL 的 LIMIT 语法；统一由 {@link com.xuanchen.common.datasource.QueryRunner}
 * 通过 JDBC {@code PreparedStatement.setMaxRows} 实施，保证跨方言一致且无法被配置绕过。
 * 本类只把用户 SQL 里显式写出的 LIMIT 数值钳制到 maxRows，防止「LIMIT 99999999」放大上限。
 * <p>
 * 参数一律走 PreparedStatement 绑定（调用方负责把占位符替换为 ?），本类不做参数拼接。
 * 放 common 模块 → 所有业务模块零依赖使用。
 *
 * @author XuanChen
 * @date 2026-10-22
 */
public final class SqlGuard {

    private SqlGuard() {
    }

    /** 允许的 JDBC 连接串协议前缀（与 sys_datasource 支持的数据源类型对齐） */
    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("^(jdbc:(mysql|mariadb|postgresql|oracle|sqlserver|dm|kingbase|h2)://|jdbc:oracle:thin:@|jdbc:dm://|jdbc:kingbase8://|jdbc:sqlserver://).*",
                    Pattern.CASE_INSENSITIVE);

    /** 行注释 / 块注释剥离 */
    private static final Pattern LINE_COMMENT = Pattern.compile("--[^\\n]*");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * 危险关键字黑名单（注释剥离后整词匹配，大小写不敏感）。
     * 去掉 information_schema：它是 PostgreSQL / MySQL 的标准元数据视图，一刀切会误伤合法查询，
     * 元数据暴露风险由「数据源只读账号 + 管理员配置 SQL」这一层约束。
     * 补充跨方言的文件读取 / 外部网络 / 命令执行函数，避免黑名单只覆盖 MySQL。
     */
    private static final Pattern FORBIDDEN_KEYWORDS = Pattern.compile(
            "\\b(insert|update|delete|merge|truncate|drop|alter|create|rename|grant|revoke"
                    + "|call|exec|execute|use|lock|unlock|load_file|outfile|dumpfile"
                    + "|shutdown|kill|sleep|benchmark|mysql\\."
                    + "|pg_read_file|pg_read_binary_file|pg_ls_dir|pg_sleep|lo_import|lo_export|dblink"
                    + "|utl_http|utl_file|dbms_lob|dbms_scheduler|xp_cmdshell|openrowset|opendatasource)\\b",
            Pattern.CASE_INSENSITIVE);

    /** DDL 场景专用黑名单：允许 create/alter/drop table 本身，只拦 DML 与危险函数 */
    private static final Pattern DDL_FORBIDDEN_KEYWORDS = Pattern.compile(
            "\\b(insert|update|delete|merge|truncate|grant|revoke|call|exec|execute"
                    + "|load_file|outfile|dumpfile|shutdown|kill|sleep|benchmark"
                    + "|pg_read_file|pg_ls_dir|utl_http|utl_file|xp_cmdshell)\\b",
            Pattern.CASE_INSENSITIVE);

    /** 末尾分号（只允许单条语句） */
    private static final Pattern TRAILING_SEMICOLON = Pattern.compile(";+\\s*$");
    /** LIMIT 100 或 LIMIT 10,100（MySQL 分页写法） */
    private static final Pattern LIMIT_CLAUSE =
            Pattern.compile("\\blimit\\s+(\\d+)\\s*(?:,\\s*(\\d+)\\s*)?", Pattern.CASE_INSENSITIVE);

    /**
     * 校验并清洗数据集 SQL：只读校验 + 钳制显式 LIMIT，不改写 SQL 结构（跨方言安全）。
     *
     * @param sqlText 原始 SQL
     * @param maxRows 最大返回行数（仅用于钳制已有 LIMIT；真正的上限由 JDBC setMaxRows 保证）
     * @return 去注释、去末尾分号、LIMIT 数值已钳制的 SQL
     */
    public static String validateSelect(String sqlText, int maxRows) {
        if (sqlText == null || sqlText.isBlank()) {
            throw new BusinessException(400, "查询 SQL 不能为空！");
        }
        String cleaned = LINE_COMMENT.matcher(sqlText).replaceAll(" ");
        cleaned = BLOCK_COMMENT.matcher(cleaned).replaceAll(" ");
        cleaned = TRAILING_SEMICOLON.matcher(cleaned).replaceAll(" ").trim();
        if (cleaned.contains(";")) {
            throw new BusinessException(400, "查询 SQL 只允许单条语句！");
        }
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("select") && !lower.startsWith("with")) {
            throw new BusinessException(400, "查询 SQL 只允许 SELECT/WITH 查询！");
        }
        if (FORBIDDEN_KEYWORDS.matcher(cleaned).find()) {
            throw new BusinessException(400, "查询 SQL 含有被禁止的关键字！");
        }
        return clampLimit(cleaned, Math.max(1, maxRows));
    }

    /** 仅做语法/只读校验，不改变 SQL（用于保存配置时的校验） */
    public static void validateSelectSyntax(String sqlText) {
        validateSelect(sqlText, Integer.MAX_VALUE);
    }

    /** 把显式 LIMIT 的返回条数钳制到 maxRows，兼容 LIMIT n 与 LIMIT offset,n */
    private static String clampLimit(String sql, int maxRows) {
        Matcher matcher = LIMIT_CLAUSE.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String first = matcher.group(1);
            String second = matcher.group(2);
            String replacement;
            if (second == null) {
                replacement = "LIMIT " + Math.min(parseLongSafe(first), maxRows);
            } else {
                replacement = "LIMIT " + Math.min(parseLongSafe(first), maxRows)
                        + "," + Math.min(parseLongSafe(second), maxRows);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /** 校验 JDBC 连接串协议，只放行白名单数据库 */
    public static void validateJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || !JDBC_URL_PATTERN.matcher(jdbcUrl.trim().toLowerCase(Locale.ROOT)).matches()) {
            throw new BusinessException(400, "不支持的 JDBC 连接串协议！");
        }
    }

    /**
     * 校验 dsType 与 JDBC 连接串协议是否一致（避免出现「选 mysql 却连 postgresql」）。
     * mariadb 按 mysql 处理，postgres/pg 归一到 postgresql，kingbase8 归一到 kingbase。
     */
    public static void validateTypeMatchesUrl(String dsType, String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }
        String protocol = protocolOf(jdbcUrl);
        if (protocol == null) {
            throw new BusinessException(400, "不支持的 JDBC 连接串协议！");
        }
        String normalized = dsType == null ? "" : dsType.trim().toLowerCase(Locale.ROOT);
        boolean matched = switch (normalized) {
            case "mysql", "mariadb" -> "mysql".equals(protocol) || "mariadb".equals(protocol);
            case "postgresql", "postgres", "pg" -> "postgresql".equals(protocol);
            case "oracle" -> "oracle".equals(protocol);
            case "sqlserver" -> "sqlserver".equals(protocol);
            case "dm" -> "dm".equals(protocol);
            case "kingbase", "kingbase8" -> "kingbase".equals(protocol);
            case "h2" -> "h2".equals(protocol);
            default -> true; // 未知类型交给方言层判定
        };
        if (!matched) {
            throw new BusinessException(400, "数据源类型与 JDBC 连接串协议不一致：dsType=" + dsType
                    + "，连接串协议=" + protocol);
        }
    }

    private static String protocolOf(String jdbcUrl) {
        String lower = jdbcUrl.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("jdbc:mysql://")) return "mysql";
        if (lower.startsWith("jdbc:mariadb://")) return "mariadb";
        if (lower.startsWith("jdbc:postgresql://")) return "postgresql";
        if (lower.startsWith("jdbc:oracle:thin:@") || lower.startsWith("jdbc:oracle://")) return "oracle";
        if (lower.startsWith("jdbc:sqlserver://")) return "sqlserver";
        if (lower.startsWith("jdbc:dm://")) return "dm";
        if (lower.startsWith("jdbc:kingbase8://") || lower.startsWith("jdbc:kingbase://")) return "kingbase";
        if (lower.startsWith("jdbc:h2://")) return "h2";
        return null;
    }

    /**
     * 把 SQL 中的 #{param} 或 :param 命名占位符替换为 ?，并按出现顺序返回参数名列表。
     * 跳过字符串字面量与 PostgreSQL 的 :: 类型转换，避免把 '12:30' 或 ::text 误当参数。
     *
     * @return [0]=替换后的 SQL，[1]=参数名数组
     */
    public static Object[] extractParams(String sqlText) {
        if (sqlText == null) {
            return new Object[]{"", new String[0]};
        }
        List<String> names = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < sqlText.length(); i++) {
            char c = sqlText.charAt(i);
            char next = i + 1 < sqlText.length() ? sqlText.charAt(i + 1) : '\0';
            if (c == '\'' && !inDouble) {
                if (inSingle && next == '\'') {
                    sb.append(c).append(next);
                    i++;
                    continue;
                }
                inSingle = !inSingle;
                sb.append(c);
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                sb.append(c);
                continue;
            }
            if (!inSingle && !inDouble && c == '#' && next == '{') {
                int end = sqlText.indexOf('}', i + 2);
                if (end > i + 2) {
                    String name = sqlText.substring(i + 2, end).trim();
                    if (name.matches("[a-zA-Z0-9_]+")) {
                        names.add(name);
                        sb.append('?');
                        i = end;
                        continue;
                    }
                }
            }
            if (!inSingle && !inDouble && c == ':' && (Character.isLetterOrDigit(next) || next == '_')
                    && (i == 0 || sqlText.charAt(i - 1) != ':')) {
                int j = i + 1;
                while (j < sqlText.length() && (Character.isLetterOrDigit(sqlText.charAt(j)) || sqlText.charAt(j) == '_')) {
                    j++;
                }
                names.add(sqlText.substring(i + 1, j));
                sb.append('?');
                i = j - 1;
                continue;
            }
            sb.append(c);
        }
        return new Object[]{sb.toString(), names.toArray(new String[0])};
    }

    /**
     * 校验 DDL 语句（代码生成器/初始化场景专用）。
     * 允许 CREATE/ALTER/DROP TABLE，禁止 DML、DROP DATABASE、GRANT、TRUNCATE 等危险语句。
     * 多条语句用分号分隔，逐条校验。
     */
    public static void validateDdl(String ddlText) {
        if (ddlText == null || ddlText.isBlank()) {
            throw new BusinessException(400, "DDL 不能为空！");
        }
        String cleaned = LINE_COMMENT.matcher(ddlText).replaceAll(" ");
        cleaned = BLOCK_COMMENT.matcher(cleaned).replaceAll(" ");
        for (String stmt : cleaned.split(";")) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("create table")
                    && !lower.startsWith("alter table")
                    && !lower.startsWith("drop table")
                    && !lower.startsWith("comment on table")
                    && !lower.startsWith("comment on column")) {
                throw new BusinessException(400, "DDL 只允许 CREATE/ALTER/DROP TABLE / COMMENT ON，禁止：" + trimmed.substring(0, Math.min(80, trimmed.length())));
            }
            if (DDL_FORBIDDEN_KEYWORDS.matcher(trimmed).find()) {
                throw new BusinessException(400, "DDL 含有被禁止的关键字：" + trimmed.substring(0, Math.min(80, trimmed.length())));
            }
        }
    }
}
