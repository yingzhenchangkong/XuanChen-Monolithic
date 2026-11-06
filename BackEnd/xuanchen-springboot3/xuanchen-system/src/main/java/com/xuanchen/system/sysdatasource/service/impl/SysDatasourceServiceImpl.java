package com.xuanchen.system.sysdatasource.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.system.sysdatasource.entity.SysDatasource;
import com.xuanchen.system.sysdatasource.mapper.SysDatasourceMapper;
import com.xuanchen.system.sysdatasource.service.ISysDatasourceService;
import com.xuanchen.common.utils.SqlGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Service接口实现类-->报表数据源
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Slf4j
@Service
public class SysDatasourceServiceImpl extends ServiceImpl<SysDatasourceMapper, SysDatasource> implements ISysDatasourceService {

    /**
     * 连接测试：入参对象的 password 约定为<b>明文</b>——
     * 密文→明文的转换统一由 controller 完成，避免重复解密把口令变成空串。
     */
    @Override
    public String testConnection(SysDatasource datasource) {
        if (datasource == null || datasource.getJdbcUrl() == null || datasource.getJdbcUrl().isBlank()) {
            return "JDBC 连接串不能为空！";
        }
        SqlGuard.validateJdbcUrl(datasource.getJdbcUrl());
        SqlGuard.validateTypeMatchesUrl(datasource.getDsType(), datasource.getJdbcUrl());
        try {
            Properties props = new Properties();
            if (datasource.getUsername() != null && !datasource.getUsername().isBlank()) {
                props.setProperty("user", datasource.getUsername());
            }
            if (datasource.getPassword() != null && !datasource.getPassword().isBlank()) {
                props.setProperty("password", datasource.getPassword());
            }
            props.setProperty("connectTimeout", "5000");
            props.setProperty("socketTimeout", "10000");
            try (Connection conn = DriverManager.getConnection(datasource.getJdbcUrl(), props)) {
                conn.isValid(5);
                return null;
            }
        } catch (Exception e) {
            log.warn("统一数据源连接测试失败: {}", e.getMessage());
            return "连接失败：" + e.getMessage();
        }
    }
}
