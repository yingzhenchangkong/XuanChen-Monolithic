package com.xuanchen.common.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.SqlGuard;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部数据源连接工厂：按数据源 id 复用连接池，替换此前四处重复的
 * {@code DriverManager.getConnection}（每次查询都要重新 TCP 握手 + 认证）。
 * <p>
 * 池实现沿用项目已有的 Druid（common 已依赖 druid-spring-boot-3-starter，
 * 且 mybatis-plus 里显式排除了 HikariCP，故不引入新连接池）。
 * <ul>
 *   <li>每数据源一个小池：maxActive=5、minIdle=0、空闲 60s 回收，读多写少的取数场景足够</li>
 *   <li>池总数上限 20，超出按最近最少使用淘汰并关闭，防止数据源配置过多导致线程/句柄泄漏</li>
 *   <li>JDBC 连接串协议白名单统一在 {@link SqlGuard#validateJdbcUrl} 一处校验</li>
 *   <li>数据源被修改/停用/删除时由 system 模块调用 {@link #evict(String)} 立即失效</li>
 * </ul>
 *
 * @author XuanChen
 * @date 2026-11-05
 */
@Slf4j
@Component
public class DatasourceConnectionFactory {

    /** 单数据源最大连接数（报表/大屏取数是低频短查询，不需要大池） */
    private static final int MAX_ACTIVE = 5;
    /** 池缓存上限，超出按 LRU 淘汰 */
    private static final int MAX_POOLS = 20;
    /** 取连接最长等待（毫秒） */
    private static final long MAX_WAIT_MS = 5000;
    /** 连接空闲回收阈值（毫秒） */
    private static final long IDLE_TIMEOUT_MS = 60_000;
    /** 连接最长存活（毫秒） */
    private static final long MAX_LIFETIME_MS = 30 * 60_000L;

    private final Map<String, Pooled> pools = new ConcurrentHashMap<>();

    /**
     * 取外部数据源连接（调用方负责 close，Druid 会归还到池）。
     *
     * @param key 池键，取数据源 id
     * @param info 数据源信息（密码为明文）
     */
    public Connection getConnection(String key, DatasourceInfo info) {
        if (info == null) {
            throw new BusinessException(500, "数据源不存在或已被删除！");
        }
        SqlGuard.validateJdbcUrl(info.getJdbcUrl());
        // 配置以首次取连接时的信息固化；若口令/连接串已变化（例如外部改库未走 evict），
        // 主动弃池重建，避免旧凭据一直被复用。
        String fingerprint = fingerprint(info);
        Pooled pooled = pools.get(key);
        if (pooled != null && !pooled.fingerprint.equals(fingerprint)) {
            evict(key);
            pooled = null;
        }
        if (pooled == null) {
            pooled = pools.computeIfAbsent(key, k -> new Pooled(createDataSource(k, info), fingerprint));
        }
        pooled.lastUsed = System.currentTimeMillis();
        trimIfNeeded(key);
        try {
            return pooled.dataSource.getConnection();
        } catch (SQLException e) {
            // 连接失败时丢弃该池，下次重建（配置改错后改回来不必重启）
            evict(key);
            log.error("[DS-POOL] 获取连接失败 dsId={} url={}", key, info.getJdbcUrl(), e);
            throw new BusinessException(500, "连接外部数据源失败：" + e.getMessage());
        }
    }

    /**
     * 失效某个数据源的连接池（修改/停用/删除时调用）。
     */
    public void evict(String key) {
        if (key == null) {
            return;
        }
        Pooled pooled = pools.remove(key);
        if (pooled != null) {
            close(pooled.dataSource);
            log.info("[DS-POOL] 已关闭数据源连接池 dsId={}", key);
        }
    }

    /**
     * 失效全部连接池（数据源批量变更时调用）。
     */
    public void evictAll() {
        List<String> keys = new ArrayList<>(pools.keySet());
        keys.forEach(this::evict);
    }

    @PreDestroy
    public void destroy() {
        evictAll();
    }

    private DruidDataSource createDataSource(String key, DatasourceInfo info) {
        DruidDataSource ds = new DruidDataSource();
        ds.setName("xc-ds-" + key);
        ds.setUrl(info.getJdbcUrl());
        ds.setUsername(info.getUsername());
        ds.setPassword(info.getPassword());
        ds.setInitialSize(0);
        ds.setMinIdle(0);
        ds.setMaxActive(MAX_ACTIVE);
        ds.setMaxWait(MAX_WAIT_MS);
        ds.setTimeBetweenEvictionRunsMillis(30_000L);
        ds.setMinEvictableIdleTimeMillis(IDLE_TIMEOUT_MS);
        ds.setMaxEvictableIdleTimeMillis(MAX_LIFETIME_MS);
        // 外部库不保证支持 validationQuery，关闭探活避免误判/额外往返
        ds.setTestOnBorrow(false);
        ds.setTestWhileIdle(false);
        ds.setValidationQuery(null);
        ds.setPoolPreparedStatements(false);
        ds.setKeepAlive(false);
        ds.setRemoveAbandoned(false);
        ds.setConnectProperties(connectProperties());
        log.info("[DS-POOL] 创建数据源连接池 dsId={} type={} url={}", key, info.getDsType(), info.getJdbcUrl());
        return ds;
    }

    private Properties connectProperties() {
        Properties props = new Properties();
        props.setProperty("connectTimeout", "5000");
        props.setProperty("socketTimeout", "30000");
        return props;
    }

    /** 池数量超过上限时，淘汰最久未使用的（跳过当前正在使用的 key） */
    private void trimIfNeeded(String currentKey) {
        if (pools.size() <= MAX_POOLS) {
            return;
        }
        pools.entrySet().stream()
                .filter(e -> !e.getKey().equals(currentKey))
                .min(Comparator.comparingLong(e -> e.getValue().lastUsed))
                .map(Map.Entry::getKey)
                .ifPresent(this::evict);
    }

    private void close(DruidDataSource dataSource) {
        try {
            dataSource.close();
        } catch (Exception e) {
            log.warn("[DS-POOL] 关闭连接池异常", e);
        }
    }

    private static String fingerprint(DatasourceInfo info) {
        return String.valueOf(info.getJdbcUrl()) + "|"
                + String.valueOf(info.getUsername()) + "|"
                + String.valueOf(info.getPassword());
    }

    private static final class Pooled {
        private final DruidDataSource dataSource;
        private final String fingerprint;
        private volatile long lastUsed = System.currentTimeMillis();

        private Pooled(DruidDataSource dataSource, String fingerprint) {
            this.dataSource = dataSource;
            this.fingerprint = fingerprint;
        }
    }
}
