package com.xuanchen.common.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一数据源轻量 DTO（业务模块通过 DatasourceProvider SPI 进程内获取，不经 HTTP）。
 * 字段覆盖 report/api/visualization/generator 四模块所需连接信息。
 *
 * @author XuanChen
 * @date 2026-11-06
 */
@Data
public class DatasourceInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    /** 数据源名称 */
    private String dsName;
    /** 数据源类型：mysql/postgresql/oracle/sqlserver/dm/kingbase/h2 */
    private String dsType;
    /** JDBC 连接串 */
    private String jdbcUrl;
    /** 用户名 */
    private String username;
    /** 密码（内部调用时为明文） */
    private String password;
    /** 状态：1=启用 */
    private Integer status;
}
