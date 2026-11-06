package com.xuanchen.generator.dialect;

import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 数据库方言：收敛不同数据库在「连接 URL / 元数据查询 / 建表 DDL」上的差异。
 * <p>
 * 元数据统一归一化成本项目内部使用的 MySQL 风格类型名（int / bigint / varchar / datetime ...），
 * 这样上层的 Java 类型映射与界面控件推断无需感知数据库差异。
 *
 * @author XuanChen
 * @date 2026-10-19
 */
public interface DatabaseDialect {

    /**
     * 方言标识（与 sys_datasource.ds_type 对齐）
     */
    String key();

    /**
     * 构建 JDBC URL（连接属性由方言固定，调用方无法追加）
     */
    String buildUrl(String host, String port, String dbName);

    /**
     * 查询表列表
     *
     * @return 每项包含 tableName / tableComment
     */
    List<Map<String, Object>> listTables(Connection conn, String dbName) throws SQLException;

    /**
     * 查询字段元数据
     *
     * @return 每项包含 columnName / columnComment / columnType / columnLength / columnScale /
     * columnDefault / columnIsPk / columnIsNullable / extra
     */
    List<Map<String, Object>> listColumns(Connection conn, String dbName, String tableName) throws SQLException;

    /**
     * 按字段配置生成建表语句（可能是多条，如 PostgreSQL 的 COMMENT ON）
     *
     * @param table   表配置
     * @param columns 字段配置
     * @return 可依次执行的 SQL 列表
     */
    List<String> buildCreateTable(GenTable table, List<GenTableColumn> columns);
}
