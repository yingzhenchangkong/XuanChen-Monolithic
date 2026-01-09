package com.xuanchen.generator.entity;

import lombok.Data;

/**
 * 实体类 --> 代码生成器
 *
 * @author XuanChen
 * @date 2025-03-04
 */
@Data
public class CodeGenerator {
    /**
     * 数据库连接地址
     */
    private String url;
    /**
     * 数据库名称
     */
    private String databaseName;
    /**
     * 数据库连接用户名
     */
    private String username;
    /**
     * 数据库连接密码
     */
    private String password;
    /**
     * 输出目录
     */
    private String outputDir;
    /**
     * 模块名
     */
    private String moduleName;
    /**
     * 包名
     */
    private String packageName;
    /**
     * 表名
     */
    private String tableName;
}
