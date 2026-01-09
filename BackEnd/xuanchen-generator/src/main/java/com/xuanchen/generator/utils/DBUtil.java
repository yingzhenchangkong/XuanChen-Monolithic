package com.xuanchen.generator.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.xuanchen.generator.entity.CodeGenerator;

import java.sql.Connection;

/**
 * 工具类-->数据库
 *
 * @author XuanChen
 * @date 2025-09-28
 */
public class DBUtil {
    public static Connection getConnection(CodeGenerator codeGenerator,DruidDataSource druidDataSource) {
        String driverClassName = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://" + codeGenerator.getUrl() + "/" + codeGenerator.getDatabaseName() + "?serverTimezone=GMT%2B8&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false&tinyInt1isBit=true";
        String username = codeGenerator.getUsername();
        String password = codeGenerator.getPassword();
        druidDataSource = new DruidDataSource();
        Connection conn = null;
        try {
            druidDataSource.setDriverClassName(driverClassName);
            druidDataSource.setUrl(url);
            druidDataSource.setUsername(username);
            druidDataSource.setPassword(password);
            druidDataSource.setMaxActive(10);
            conn = druidDataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
}
