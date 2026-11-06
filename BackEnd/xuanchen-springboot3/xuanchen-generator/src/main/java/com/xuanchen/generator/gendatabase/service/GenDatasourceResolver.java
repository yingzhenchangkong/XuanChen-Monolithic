package com.xuanchen.generator.gendatabase.service;

import com.xuanchen.common.datasource.DatasourceProvider;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.entity.DatasourceOption;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.SqlGuard;
import com.xuanchen.generator.gendatabase.entity.GenDatasource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据源解析器：通过 {@link DatasourceProvider}（进程内接口，system 模块实现）获取统一数据源信息。
 * <p>
 * 代码生成方言仅支持 mysql / mariadb / postgresql，其余 dsType 的数据源不进入下拉。
 * 解析结果为瞬态 {@link GenDatasource}，密码已解密为明文。
 *
 * @author XuanChen
 * @date 2026-11-05
 */
@Component
public class GenDatasourceResolver {

    /**
     * 代码生成方言仅支持 mysql / postgresql，mariadb 按 mysql 处理
     */
    private static final List<String> SUPPORTED_DS_TYPES = List.of("mysql", "mariadb", "postgresql");

    /**
     * 从 JDBC URL 提取库名：jdbc:mysql://host:3306/dbname?x=y / jdbc:postgresql://host/db
     */
    private static final Pattern DB_NAME_PATTERN =
            Pattern.compile("^jdbc:(mysql|mariadb|postgresql)://[^/?#\\s]+/([A-Za-z0-9_$\\-]{1,64})", Pattern.CASE_INSENSITIVE);

    private final DatasourceProvider datasourceProvider;

    public GenDatasourceResolver(DatasourceProvider datasourceProvider) {
        this.datasourceProvider = datasourceProvider;
    }

    /**
     * 按 id 解析数据源（即 sys_datasource.id），校验存在性、启用状态、方言支持。
     */
    public GenDatasource resolve(String databaseId) {
        if (databaseId == null || databaseId.isBlank()) {
            throw new BusinessException(400, "数据源 id 不能为空");
        }
        DatasourceInfo ds = datasourceProvider.get(databaseId);
        SqlGuard.validateJdbcUrl(ds.getJdbcUrl());
        String connType = mapConnType(ds.getDsType());
        if (connType == null) {
            throw new BusinessException(400, "代码生成暂不支持该数据源类型：" + ds.getDsType()
                    + "（当前支持 mysql / postgresql）");
        }
        GenDatasource db = new GenDatasource();
        db.setId(ds.getId());
        db.setConnType(connType);
        db.setConnName(ds.getDsName());
        db.setJdbcUrl(ds.getJdbcUrl());
        db.setDbName(extractDbName(ds.getJdbcUrl()));
        db.setUserName(ds.getUsername());
        db.setPassword(ds.getPassword());
        db.setStatus(ds.getStatus());
        return db;
    }

    /**
     * 下拉候选：sys_datasource 中方言支持且启用的数据源。连接串与密码不回传前端。
     */
    public List<GenDatasource> select() {
        List<DatasourceOption> all = datasourceProvider.listOptions();
        List<GenDatasource> list = new ArrayList<>();
        for (DatasourceOption ds : all) {
            if (!SUPPORTED_DS_TYPES.contains(ds.getDsType() == null ? "" : ds.getDsType().toLowerCase())) {
                continue;
            }
            GenDatasource db = new GenDatasource();
            db.setId(ds.getId());
            db.setConnType(mapConnType(ds.getDsType()));
            db.setConnName(ds.getDsName());
            list.add(db);
        }
        return list;
    }

    private String mapConnType(String dsType) {
        if (dsType == null) {
            return null;
        }
        return switch (dsType.trim().toLowerCase()) {
            case "mysql", "mariadb" -> "mysql";
            case "postgresql", "postgres", "pg" -> "postgresql";
            default -> null;
        };
    }

    private String extractDbName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return null;
        }
        Matcher matcher = DB_NAME_PATTERN.matcher(jdbcUrl.trim());
        return matcher.find() ? matcher.group(2) : null;
    }
}
