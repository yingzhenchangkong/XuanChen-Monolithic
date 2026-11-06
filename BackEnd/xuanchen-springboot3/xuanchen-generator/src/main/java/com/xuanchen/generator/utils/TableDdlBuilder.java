package com.xuanchen.generator.utils;

import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 工具类 --> MySQL 建表语句生成。
 * <p>
 * 「是否创建表 = 是」表示目标数据源里还没有这张表，字段结构完全来自 gen_table_column 配置，
 * 由本类拼成 {@code CREATE TABLE IF NOT EXISTS} 交给 {@link DBUtil#executeDdl} 执行。
 * <p>
 * 安全约束：前端只能提交结构化字段（列名/类型/长度/默认值），本类负责
 * <ol>
 *     <li>列名必须是合法数据库标识符（字母/数字/下划线，字母或下划线开头）；</li>
 *     <li>字段类型必须在 MySQL 类型白名单内（避免 {@code int); DROP TABLE ...} 这类注入）；</li>
 *     <li>长度/精度必须是数字且有上限，默认值/注释统一转义单引号与反斜杠。</li>
 * </ol>
 *
 * @author XuanChen
 * @date 2026-10-18
 */
public final class TableDdlBuilder {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");

    /** 允许的字段类型（MySQL 8 常用类型；不在白名单内一律拒绝） */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "tinyint", "smallint", "mediumint", "int", "integer", "bigint",
            "decimal", "numeric", "float", "double",
            "char", "varchar", "tinytext", "text", "mediumtext", "longtext",
            "binary", "varbinary", "blob", "mediumblob", "longblob",
            "date", "datetime", "timestamp", "time", "year",
            "bit", "bool", "boolean", "json");

    /** 需要写长度的类型 */
    private static final Set<String> LENGTH_TYPES = Set.of("char", "varchar", "binary", "varbinary");

    /** 支持 (精度, 小数位) 的类型 */
    private static final Set<String> SCALE_TYPES = Set.of("decimal", "numeric");

    /** 整数类型（单主键时自动追加 AUTO_INCREMENT） */
    private static final Set<String> INT_TYPES = Set.of("tinyint", "smallint", "mediumint", "int", "integer", "bigint");

    /** 默认值可直接写裸值的类型（数字/布尔） */
    private static final Set<String> NUMERIC_TYPES = Set.of(
            "tinyint", "smallint", "mediumint", "int", "integer", "bigint",
            "decimal", "numeric", "float", "double", "bit", "bool", "boolean");

    private static final int MAX_LENGTH = 65535;
    private static final int MAX_SCALE = 30;

    private TableDdlBuilder() {
    }

    /**
     * 按字段配置生成建表语句。
     *
     * @param table   表配置（表名/注释）
     * @param columns 字段配置（至少一个主键字段）
     * @return 可直接执行的 CREATE TABLE 语句
     */
    public static String build(GenTable table, List<GenTableColumn> columns) {
        String tableName = table.getTableName();
        if (tableName == null || !IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            throw new BusinessException(400, "表名仅允许字母、数字、下划线，且必须以字母或下划线开头（最长 64 位）");
        }
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException(400, "已选择「是否创建表 = 是」，但没有配置任何字段，"
                    + "请先在「数据库表配置」页签添加字段（至少包含一个主键字段）");
        }

        List<String> pkColumns = new ArrayList<>();
        // 先收集主键列：单主键整数列要追加 AUTO_INCREMENT，必须先知道主键总数
        for (GenTableColumn column : columns) {
            String columnName = column.getColumnName();
            if (columnName == null || !IDENTIFIER_PATTERN.matcher(columnName).matches()) {
                throw new BusinessException(400, "字段名「" + columnName
                        + "」不合法：仅允许字母、数字、下划线，且必须以字母或下划线开头");
            }
            if (isTrue(column.getColumnIsPk())) {
                pkColumns.add(columnName);
            }
        }
        if (pkColumns.isEmpty()) {
            throw new BusinessException(400, "建表至少需要一个主键字段，请在「数据库表配置」中勾选一个字段的「是否主键」");
        }

        List<String> definitions = new ArrayList<>();
        for (GenTableColumn column : columns) {
            boolean pk = isTrue(column.getColumnIsPk());
            definitions.add(definition(column, pk, pkColumns.size() == 1 && pk));
        }
        definitions.add("PRIMARY KEY (" + quoteAll(pkColumns) + ")");

        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ").append(quote(tableName)).append(" (\n  ")
                .append(String.join(",\n  ", definitions))
                .append("\n) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
        String comment = table.getTableComment();
        if (comment != null && !comment.isBlank()) {
            ddl.append(" COMMENT = '").append(escape(comment)).append('\'');
        }
        return ddl.toString();
    }

    /**
     * 单个字段定义：`name` type [NOT NULL|NULL] [DEFAULT x] [AUTO_INCREMENT] COMMENT '...'
     */
    private static String definition(GenTableColumn column, boolean pk, boolean autoIncrement) {
        String type = normalizeType(column.getColumnType());
        StringBuilder sb = new StringBuilder();
        sb.append(quote(column.getColumnName())).append(' ').append(typeExpr(type, column));

        if (pk) {
            sb.append(" NOT NULL");
        } else {
            sb.append(isTrue(column.getColumnIsNullable()) ? " NULL" : " NOT NULL");
        }

        String defaultValue = defaultValueExpr(type, column.getColumnDefault(), pk);
        if (defaultValue != null) {
            sb.append(" DEFAULT ").append(defaultValue);
        }
        // 单列整数主键默认自增：否则 MyBatis-Plus 插入时不会回填主键
        if (autoIncrement && INT_TYPES.contains(type) && defaultValue == null) {
            sb.append(" AUTO_INCREMENT");
        }
        String comment = column.getColumnComment();
        sb.append(" COMMENT '").append(escape(comment == null || comment.isBlank()
                ? column.getColumnName() : comment)).append('\'');
        return sb.toString();
    }

    private static String normalizeType(String columnType) {
        String type = columnType == null ? "" : columnType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(400, "字段类型「" + columnType + "」不受支持，请从下拉框中选择");
        }
        return type;
    }

    /**
     * 类型 + 长度/精度：varchar(64)、decimal(10,2)；其余类型只写类型名。
     */
    private static String typeExpr(String type, GenTableColumn column) {
        Integer length = column.getColumnLength();
        Integer scale = column.getColumnScale();
        if (SCALE_TYPES.contains(type)) {
            if (length != null && length > 0) {
                StringBuilder sb = new StringBuilder(type).append('(').append(Math.min(length, 65));
                if (scale != null && scale > 0) {
                    sb.append(',').append(Math.min(scale, MAX_SCALE));
                }
                return sb.append(')').toString();
            }
            return type;
        }
        if (LENGTH_TYPES.contains(type)) {
            if (length != null && length > 0) {
                return type + "(" + Math.min(length, MAX_LENGTH) + ")";
            }
            throw new BusinessException(400, "字段「" + column.getColumnName() + "」类型为 " + type
                    + "，必须填写字段长度");
        }
        return type;
    }

    /**
     * 默认值表达式；返回 null 表示不写 DEFAULT 子句。
     */
    private static String defaultValueExpr(String type, String rawDefault, boolean pk) {
        if (rawDefault == null || rawDefault.isBlank()) {
            return null;
        }
        String value = rawDefault.trim();
        String upper = value.toUpperCase(Locale.ROOT);
        if ("CURRENT_TIMESTAMP".equals(upper) || "CURRENT_TIMESTAMP()".equals(upper)
                || "NOW()".equals(upper)) {
            if (!"datetime".equals(type) && !"timestamp".equals(type)) {
                throw new BusinessException(400, "只有 datetime/timestamp 字段可以使用 CURRENT_TIMESTAMP 默认值");
            }
            return "CURRENT_TIMESTAMP";
        }
        if ("NULL".equals(upper)) {
            if (pk) {
                throw new BusinessException(400, "主键字段不能设置默认值 NULL");
            }
            return "NULL";
        }
        if (NUMERIC_TYPES.contains(type) && value.matches("-?\\d+(\\.\\d+)?")) {
            return value;
        }
        return "'" + escape(value) + "'";
    }

    private static String quote(String identifier) {
        return "`" + identifier + "`";
    }

    private static String quoteAll(List<String> identifiers) {
        List<String> quoted = new ArrayList<>();
        for (String identifier : identifiers) {
            quoted.add(quote(identifier));
        }
        return String.join(", ", quoted);
    }

    /**
     * 转义字符串字面量：反斜杠与单引号（MySQL 默认模式下两者都需要转义）。
     */
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "''");
    }

    /**
     * sb3 的实体布尔字段是 Integer（1/0），sb4 是 Boolean，这里统一兼容。
     */
    private static boolean isTrue(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() == 1;
        }
        return false;
    }
}
