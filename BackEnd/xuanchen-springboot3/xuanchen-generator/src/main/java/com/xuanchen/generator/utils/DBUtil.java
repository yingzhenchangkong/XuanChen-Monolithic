package com.xuanchen.generator.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具类-->数据库
 *
 * @author XuanChen
 * @date 2025-09-28
 */
public class DBUtil {
    /**
     * 获取数据库连接
     *
     * @param genDatabase
     * @return
     */
    public static Connection getConnection(GenDatabase genDatabase, DruidDataSource druidDataSource) {
        String driverClassName = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://" + genDatabase.getHost() + ":" + genDatabase.getPort() + "/" + genDatabase.getDbName() + "?serverTimezone=GMT%2B8&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false&tinyInt1isBit=true";
        String username = genDatabase.getUserName();
        String password = genDatabase.getPassword();
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

    /**
     * 获取数据库表列表
     *
     * @param genDatabase
     * @return
     */
    public static List<Map<String, Object>> getTableList(GenDatabase genDatabase) {
        DruidDataSource druidDataSource = new DruidDataSource();
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection conn = getConnection(genDatabase, druidDataSource);
            String strSql = "SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()";
            PreparedStatement preparedStatement = conn.prepareStatement(strSql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("tableName", resultSet.getString("TABLE_NAME"));//表名
                map.put("tableComment", resultSet.getString("TABLE_COMMENT"));//表注释
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            druidDataSource.close();
        }
        return list;
    }

    /**
     * 获取数据库表信息
     *
     * @param genDatabase
     * @return
     */
    public static List<Map<String, Object>> getTableInfo(GenDatabase genDatabase, String tableName) {
        DruidDataSource druidDataSource = new DruidDataSource();
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection conn = getConnection(genDatabase, druidDataSource);
            String strSql = "SELECT COLUMN_NAME,COLUMN_COMMENT,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,NUMERIC_SCALE,IS_NULLABLE,COLUMN_DEFAULT,COLUMN_KEY,"
                    + "EXTRA FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + genDatabase.getDbName() + "' AND TABLE_NAME = '"
                    + tableName + "' ORDER BY COLUMN_KEY DESC, ORDINAL_POSITION;";
            PreparedStatement preparedStatement = conn.prepareStatement(strSql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("columnName", resultSet.getString("COLUMN_NAME"));//字段名
                map.put("columnComment", resultSet.getString("COLUMN_COMMENT"));//字段注释
                map.put("columnType", resultSet.getString("DATA_TYPE"));//字段类型
                map.put("columnLength", resultSet.getString("CHARACTER_MAXIMUM_LENGTH"));//字段最大长度
                map.put("columnScale", resultSet.getString("NUMERIC_SCALE"));//字段小数位长度
                map.put("columnDefault", resultSet.getString("COLUMN_DEFAULT"));//字段默认值
                map.put("columnIsPk", "PRI".equals(resultSet.getString("COLUMN_KEY")));//字段是否主键
                map.put("columnIsNullable", "YES".equals(resultSet.getString("IS_NULLABLE")));//字段是否允许为NULL
                map.put("extra", resultSet.getString("EXTRA"));//扩展信息
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            druidDataSource.close();
        }
        return list;
    }
}
