package com.xuanchen.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据源下拉选项 DTO（只含前端选择必需的字段，绝不含 JDBC 连接串与口令）。
 * <p>
 * report / api / visualization / generator 四个模块的下拉统一返回本对象，
 * 避免此前直接把 {@link DatasourceInfo}（含明文密码）序列化给浏览器。
 *
 * @author XuanChen
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceOption implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    /** 数据源名称 */
    private String dsName;

    /** 数据源类型：mysql/postgresql/oracle/sqlserver/dm/kingbase/h2 */
    private String dsType;

    /** 状态：1=启用 */
    private Integer status;

    public static DatasourceOption of(DatasourceInfo info) {
        return new DatasourceOption(info.getId(), info.getDsName(), info.getDsType(), info.getStatus());
    }
}
