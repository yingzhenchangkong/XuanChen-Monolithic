package com.xuanchen.generator.gendatabase.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据源瞬态对象（不映射任何数据库表）：由 {@link com.xuanchen.generator.gendatabase.service.GenDatasourceResolver}
 * 从 sys_datasource 解析后构造，供方言匹配、元数据查询、DDL 执行等下游使用。
 * <p>
 * 统一数据源密码已在构造时用报表体系 AES 解密为明文。
 *
 * @author XuanChen
 * @date 2026-11-05
 */
@Data
public class GenDatasource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源主键（即 sys_datasource.id）
     */
    private String id;

    /**
     * 连接类型（mysql / postgresql，统一由 dsType 映射而来）
     */
    private String connType;

    /**
     * 数据源名称（即 sys_datasource.dsName）
     */
    private String connName;

    /**
     * JDBC 连接串（完整 URL，直接供 DriverManager 使用）
     */
    private String jdbcUrl;

    /**
     * 数据库名（从 JDBC URL 自动提取）
     */
    private String dbName;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码（已解密为明文）
     */
    private String password;

    /**
     * 状态（1启用 0停用）
     */
    private Integer status;
}
