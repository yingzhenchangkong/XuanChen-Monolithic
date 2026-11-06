package com.xuanchen.common.datasource;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 统一取数限额：报表 / 接口管理 / 数据大屏三处取数共用同一份配置，
 * 避免各模块各自硬编码 10000 / 2000 导致策略不一致。
 *
 * @author XuanChen
 * @date 2026-11-06
 */
@Getter
@Component
public class QueryLimits {

    /** 单次查询最大返回行数（由 JDBC setMaxRows 强制，SQL 层无法绕过） */
    @Value("${xuanchen.query.max-rows:2000}")
    private int maxRows;

    /** 单次查询超时（秒） */
    @Value("${xuanchen.query.timeout-seconds:30}")
    private int timeoutSeconds;
}
