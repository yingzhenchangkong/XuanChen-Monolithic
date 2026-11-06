package com.xuanchen.ai.config;

import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;

/**
 * 向量库数据源包装：
 * 不直接注册 DataSource 类型 Bean（避免与主库 Druid 数据源在 MyBatis-Plus
 * 按类型注入时产生多候选冲突）；close() 方法由 Spring 容器销毁阶段自动调用，
 * 释放向量库连接池（Druid 关闭时同步从全局 StatManager 注销）。
 */
public class AiVectorDataSource implements AutoCloseable {

    private final DruidDataSource druid;

    public AiVectorDataSource(DruidDataSource druid) {
        this.druid = druid;
    }

    public DataSource getDataSource() {
        return druid;
    }

    @Override
    public void close() {
        druid.close();
    }
}
