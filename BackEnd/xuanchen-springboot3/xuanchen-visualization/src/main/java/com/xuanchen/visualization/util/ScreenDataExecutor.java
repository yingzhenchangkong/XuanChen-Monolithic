package com.xuanchen.visualization.util;

import com.xuanchen.common.datasource.DatasourceConnectionFactory;
import com.xuanchen.common.datasource.DatasourceProvider;
import com.xuanchen.common.datasource.QueryLimits;
import com.xuanchen.common.datasource.QueryRunner;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.SqlGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 大屏取数执行器：支持系统主库 + 外部数据源（sys_datasource 表配置）。
 * 取数委托 common 的 QueryRunner，与报表、接口管理共用 QueryLimits 与结果归一化。
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Slf4j
@Component
public class ScreenDataExecutor {

    private final DataSource dataSource;

    @Autowired
    private DatasourceProvider datasourceProvider;

    @Autowired
    private DatasourceConnectionFactory connectionFactory;

    @Autowired
    private QueryLimits queryLimits;

    public ScreenDataExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record ScreenData(List<String> columns, List<Map<String, Object>> rows) {}

    /** 主库执行 */
    public ScreenData execute(String sql, List<Object> params) {
        try (Connection conn = dataSource.getConnection()) {
            return doExecute(conn, sql, params);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("大屏主库数据集执行失败", e);
            throw new BusinessException(500, "数据集执行失败：" + e.getMessage());
        }
    }

    /** 外部数据源执行：连接由 DatasourceConnectionFactory 按数据源 id 复用 */
    public ScreenData executeExternal(String dsId, String sql, List<Object> params) {
        try {
            DatasourceInfo ds = datasourceProvider.get(dsId);
            log.info("[SCREEN-DS] external datasource dsId={} jdbcUrl={}", dsId, ds.getJdbcUrl());
            try (Connection conn = connectionFactory.getConnection(ds.getId(), ds)) {
                return doExecute(conn, sql, params);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("大屏外部数据集执行失败 dsId={}", dsId, e);
            throw new BusinessException(500, "外部数据源数据集执行失败：" + e.getMessage());
        }
    }

    private ScreenData doExecute(Connection conn, String sql, List<Object> params) throws Exception {
        Object[] extracted = SqlGuard.extractParams(sql);
        String finalSql = SqlGuard.validateSelect((String) extracted[0], queryLimits.getMaxRows());
        QueryRunner.QueryResult result = QueryRunner.run(conn, finalSql, params,
                queryLimits.getMaxRows(), queryLimits.getTimeoutSeconds());
        return new ScreenData(result.columns(), result.rows());
    }
}
