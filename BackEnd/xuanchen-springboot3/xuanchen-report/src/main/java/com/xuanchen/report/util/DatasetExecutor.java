package com.xuanchen.report.util;

import com.xuanchen.common.datasource.DatasourceConnectionFactory;
import com.xuanchen.common.datasource.QueryLimits;
import com.xuanchen.common.datasource.QueryRunner;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.SqlGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 报表数据集执行器：主库 / 外部数据源统一委托 common 的 QueryRunner 取数。
 * 行数上限、超时、值归一化与接口管理、数据大屏完全一致（QueryLimits 一处配置）。
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Slf4j
@Component
public class DatasetExecutor {

    private final DataSource dataSource;
    private final DatasourceConnectionFactory connectionFactory;
    private final QueryLimits queryLimits;

    public DatasetExecutor(DataSource dataSource, DatasourceConnectionFactory connectionFactory,
                           QueryLimits queryLimits) {
        this.dataSource = dataSource;
        this.connectionFactory = connectionFactory;
        this.queryLimits = queryLimits;
    }

    /**
     * 取数结果：columns 为列名（保持查询顺序），rows 为按列名组织的行数据。
     */
    public record DatasetResult(List<String> columns, List<Map<String, Object>> rows) {
    }

    /** 使用系统主库执行 */
    public DatasetResult executeMain(String sql, List<Object> params) {
        try (Connection conn = dataSource.getConnection()) {
            return doExecute(conn, sql, params);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("数据集执行失败(main)", e);
            throw new BusinessException(500, "数据集执行失败：" + e.getMessage());
        }
    }

    /** 使用外部数据源执行（连接由 DatasourceConnectionFactory 按数据源 id 复用） */
    public DatasetResult executeExternal(DatasourceInfo datasource, String sql, List<Object> params) {
        if (datasource == null) {
            throw new BusinessException(400, "数据集未关联有效的数据源！");
        }
        try (Connection conn = connectionFactory.getConnection(datasource.getId(), datasource)) {
            return doExecute(conn, sql, params);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("数据集执行失败(external) dsId={}", datasource.getId(), e);
            throw new BusinessException(500, "数据集执行失败：" + e.getMessage());
        }
    }

    private DatasetResult doExecute(Connection conn, String sql, List<Object> params) throws Exception {
        Object[] extracted = SqlGuard.extractParams(sql);
        String finalSql = SqlGuard.validateSelect((String) extracted[0], queryLimits.getMaxRows());
        QueryRunner.QueryResult result = QueryRunner.run(conn, finalSql, params,
                queryLimits.getMaxRows(), queryLimits.getTimeoutSeconds());
        return new DatasetResult(result.columns(), result.rows());
    }
}
