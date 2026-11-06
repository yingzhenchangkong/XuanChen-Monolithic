package com.xuanchen.system.sysdatasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.datasource.DatasourceConnectionFactory;
import com.xuanchen.common.datasource.DatasourceProvider;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.entity.DatasourceOption;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.common.utils.SysAesUtil;
import com.xuanchen.system.sysdatasource.entity.SysDatasource;
import com.xuanchen.system.sysdatasource.service.ISysDatasourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 统一数据源读取实现（{@link DatasourceProvider} 的唯一实现方）。
 * <p>
 * 缓存策略：整表快照 + 60 秒 TTL，快照里保存的是 <b>AES 密文行</b>；
 * 只有 {@link #get(String)}（真正要连库）才解密单个口令，
 * {@link #listEnabled()} / {@link #listOptions()} / {@link #getBatch(Collection)}
 * 返回的 DTO 不含密码，避免明文口令长期驻留堆内存、也避免某一条坏口令拖垮全部模块。
 * <p>
 * 多实例：写操作除本地 evict 外，还会 bump Redis 全局版本号；其他实例在 load 时
 * 发现版本变化即丢弃本地快照并关闭全部连接池，避免旧口令被无限期复用。
 *
 * @author XuanChen
 * @date 2026-11-05
 */
@Slf4j
@Service
public class SysDatasourceProvider implements DatasourceProvider {

    /** 全局版本号（Redis），用于跨实例失效 */
    public static final String VERSION_KEY = "sys:datasource:version";
    private static final String VERSION_INITIAL = "0";

    private final ISysDatasourceService datasourceService;
    private final SysAesUtil aesUtil;
    private final DatasourceConnectionFactory connectionFactory;
    private final RedisUtil redisUtil;

    /** 快照有效期（秒），0 表示不缓存 */
    @Value("${xuanchen.datasource.cache-ttl-seconds:60}")
    private long cacheTtlSeconds = 60;

    private volatile Snapshot snapshot;

    public SysDatasourceProvider(ISysDatasourceService datasourceService,
                                 SysAesUtil aesUtil,
                                 DatasourceConnectionFactory connectionFactory,
                                 RedisUtil redisUtil) {
        this.datasourceService = datasourceService;
        this.aesUtil = aesUtil;
        this.connectionFactory = connectionFactory;
        this.redisUtil = redisUtil;
    }

    @Override
    public List<DatasourceInfo> listEnabled() {
        return load().enabled();
    }

    @Override
    public List<DatasourceOption> listOptions() {
        return load().options();
    }

    @Override
    public DatasourceInfo get(String id) {
        if (StringUtil.isEmpty(id)) {
            throw new BusinessException(400, "数据源 id 不能为空！");
        }
        SysDatasource row = load().byId().get(id);
        if (row == null) {
            // 缓存未命中：可能是刚新增（写入方未及时 evict）或缓存已过期，直查一次兜底
            row = datasourceService.getById(id);
            if (row == null) {
                throw new BusinessException(400, "数据源不存在或已被删除！");
            }
        }
        if (row.getStatus() == null || row.getStatus() != 1) {
            throw new BusinessException(400, "数据源已停用！");
        }
        return toInfo(row, true);
    }

    @Override
    public Map<String, DatasourceInfo> getBatch(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, SysDatasource> all = load().byId();
        Map<String, DatasourceInfo> result = new HashMap<>(ids.size() * 2);
        for (String id : ids) {
            SysDatasource row = all.get(id);
            if (row == null && StringUtil.isNotEmpty(id)) {
                row = datasourceService.getById(id);
            }
            if (row != null) {
                result.put(id, toInfo(row, false));
            }
        }
        return result;
    }

    /**
     * 失效缓存并关闭该数据源的连接池（新增/修改/删除/停用后调用），同时广播全局版本变化。
     */
    public void evict(String id) {
        snapshot = null;
        if (StringUtil.isNotEmpty(id)) {
            connectionFactory.evict(id);
        }
        bumpVersion();
    }

    /** 失效全部缓存与连接池 */
    public void evictAll() {
        snapshot = null;
        connectionFactory.evictAll();
        bumpVersion();
    }

    // ==================== 内部实现 ====================

    private Snapshot load() {
        long now = System.currentTimeMillis();
        String version = currentVersion();
        Snapshot local = snapshot;
        if (local != null && local.expireAt() > now && Objects.equals(local.version(), version)) {
            return local;
        }
        synchronized (this) {
            local = snapshot;
            String latestVersion = currentVersion();
            if (local != null && local.expireAt() > now && Objects.equals(local.version(), latestVersion)) {
                return local;
            }
            if (local != null && !Objects.equals(local.version(), latestVersion)) {
                // 其他实例改过配置：本实例的池可能还握着旧口令，全部关闭重建
                log.info("[DS-PROVIDER] 检测到数据源配置版本变化，关闭本地连接池");
                connectionFactory.evictAll();
            }
            QueryWrapper<SysDatasource> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("create_time");
            List<SysDatasource> rows = datasourceService.list(wrapper);
            List<DatasourceInfo> enabled = new ArrayList<>(rows.size());
            List<DatasourceOption> options = new ArrayList<>(rows.size());
            Map<String, SysDatasource> byId = new HashMap<>(rows.size() * 2);
            for (SysDatasource row : rows) {
                byId.put(row.getId(), row);
                DatasourceInfo info = toInfo(row, false);
                if (info.getStatus() != null && info.getStatus() == 1) {
                    enabled.add(info);
                    options.add(DatasourceOption.of(info));
                }
            }
            long ttl = cacheTtlSeconds <= 0 ? 0 : cacheTtlSeconds * 1000L;
            snapshot = new Snapshot(Collections.unmodifiableList(enabled),
                    Collections.unmodifiableList(options),
                    Collections.unmodifiableMap(byId),
                    now + ttl, latestVersion);
            log.debug("[DS-PROVIDER] 数据源快照已刷新 总数={} 启用={}", rows.size(), enabled.size());
            return snapshot;
        }
    }

    private String currentVersion() {
        try {
            Object value = redisUtil.get(VERSION_KEY);
            return value == null ? VERSION_INITIAL : String.valueOf(value);
        } catch (Exception e) {
            log.warn("[DS-PROVIDER] 读取数据源版本失败，按本地快照处理", e);
            return VERSION_INITIAL;
        }
    }

    private void bumpVersion() {
        try {
            redisUtil.set(VERSION_KEY, UUID.randomUUID().toString());
        } catch (Exception e) {
            log.warn("[DS-PROVIDER] 广播数据源版本变化失败，其他实例可能最多延迟一个 TTL 生效", e);
        }
    }

    /** 只有需要真实连接时才解密；下拉/名称回填一律 password=null */
    private DatasourceInfo toInfo(SysDatasource row, boolean withPassword) {
        DatasourceInfo info = new DatasourceInfo();
        info.setId(row.getId());
        info.setDsName(row.getDsName());
        info.setDsType(row.getDsType());
        info.setJdbcUrl(row.getJdbcUrl());
        info.setUsername(row.getUsername());
        info.setPassword(withPassword ? decrypt(row.getPassword()) : null);
        info.setStatus(row.getStatus());
        return info;
    }

    private String decrypt(String cipherText) {
        if (StringUtil.isEmpty(cipherText)) {
            return null;
        }
        String plain = aesUtil.decryptOrNull(cipherText);
        if (plain == null) {
            log.error("[DS-PROVIDER] 数据源密码解密失败，可能是加密密钥已变更");
            throw new BusinessException(500, "数据源密码解密失败，请在「数据源配置」中重新保存该数据源");
        }
        return plain;
    }

    /** 整表快照：密文行索引 + 不含口令的启用列表/下拉项 */
    private record Snapshot(List<DatasourceInfo> enabled, List<DatasourceOption> options,
                            Map<String, SysDatasource> byId, long expireAt, String version) {
    }
}
