package com.xuanchen.monitor.cache.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.monitor.cache.entity.KeyDetailVO;
import com.xuanchen.monitor.cache.entity.RedisInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * 控制器-->Redis监控
 *
 * @author XuanChen
 * @date 2026-01-30
 */
@RestController
@RequestMapping("/monitor/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class CacheController {
    //TODO 待整理Redis信息，添加关注的，减少没意义的
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisUtil redisUtil;

    @GetMapping("/redis")
    public Result<RedisInfoVO> getRedisInfo() {
        RedisInfoVO redisInfo = getRedisDetailedInfo();
        return Result.success(redisInfo);
    }

    /**
     * 获取Redis详细信息
     */
    public RedisInfoVO getRedisDetailedInfo() {
        // 原生连接必须 try-with-resources：RedisConnection 不经 Spring 模板管理，
        // 不 close 会从连接池泄漏连接，高频访问监控页最终耗尽连接池
        try (RedisConnection connection =
                     stringRedisTemplate.getConnectionFactory().getConnection()) {

            // 获取特定部分的INFO信息
            Properties serverInfo = connection.info("server");      // 服务器信息
            Properties memoryInfo = connection.info("memory");      // 内存信息
            Properties cpuInfo = connection.info("cpu");            // CPU信息
            Properties statsInfo = connection.info("stats");        // 统计信息
            Properties clientsInfo = connection.info("clients");    // 客户端信息

            RedisInfoVO redisInfo = new RedisInfoVO();

            // 设置服务器信息
            redisInfo.setVersion(serverInfo.getProperty("redis_version"));// 获取 Redis 服务器的 版本号
            redisInfo.setMode(serverInfo.getProperty("redis_mode"));// 获取 Redis 服务器的 运行模式
            redisInfo.setOs(serverInfo.getProperty("os")); // 操作系统名称和版本
            redisInfo.setArchBits(serverInfo.getProperty("arch_bits")); // 架构(32或64位)
            redisInfo.setProcessId(serverInfo.getProperty("process_id")); // Redis进程ID

            // 设置内存信息
            redisInfo.setUsedMemory(Long.valueOf(memoryInfo.getProperty("used_memory", "0")));// 获取 Redis 实例当前使用的内存量（字节数）；这是 Redis 分配给数据的实际物理内存大小
            redisInfo.setUsedMemoryHuman(memoryInfo.getProperty("used_memory_human", "0"));// 获取人类可读格式的内存使用量；例如 "1.2G"、"512M" 等易读的格式
            redisInfo.setUsedMemoryRss(Long.valueOf(memoryInfo.getProperty("used_memory_rss", "0")));// 获取 Redis 进程的 RSS（Resident Set Size）内存使用量；RSS 是操作系统层面的内存占用，包括 Redis 数据和进程本身的内存开销
            redisInfo.setMaxMemory(Long.valueOf(memoryInfo.getProperty("maxmemory", "0")));//获取 Redis 最大可用内存限制；如果未设置内存限制，则返回 0
            redisInfo.setUsedMemoryPeak(Long.valueOf(memoryInfo.getProperty("used_memory_peak", "0"))); // 内存使用峰值
            redisInfo.setUsedMemoryPeakHuman(memoryInfo.getProperty("used_memory_peak_human", "0")); // 峰值的人类可读格式
            redisInfo.setUsedMemoryLua(Long.valueOf(memoryInfo.getProperty("used_memory_lua", "0"))); // Lua引擎占用的内存
            redisInfo.setUsedMemoryLuaHuman(memoryInfo.getProperty("used_memory_lua_human", "0")); // Lua内存的人类可读格式

            // 设置CPU信息
            redisInfo.setUsedCpuSys(Double.valueOf(cpuInfo.getProperty("used_cpu_sys", "0")));//Redis 服务器进程在内核态的 CPU 时间占比
            redisInfo.setUsedCpuUser(Double.valueOf(cpuInfo.getProperty("used_cpu_user", "0")));//Redis 服务器进程在用户态的 CPU 时间占比
            redisInfo.setUsedCpuSysChildren(Double.valueOf(cpuInfo.getProperty("used_cpu_sys_children", "0")));//Redis 后台子进程在内核态的 CPU 时间占比
            redisInfo.setUsedCpuUserChildren(Double.valueOf(cpuInfo.getProperty("used_cpu_user_children", "0")));//Redis 后台子进程在用户态的 CPU 时间占比

            // 设置客户端信息
            redisInfo.setConnectedClients(Integer.valueOf(clientsInfo.getProperty("connected_clients", "0")));//获取当前已连接到 Redis 服务器的客户端数量；这个值表示当前活跃的连接数
            redisInfo.setMaxClients(Integer.valueOf(clientsInfo.getProperty("maxclients", "0")));//获取 Redis 服务器配置的最大客户端连接数限制；该值表示 Redis 服务端设置的最大并发连接数上限

            // 设置统计信息
            redisInfo.setTotalConnectionsReceived(Long.valueOf(statsInfo.getProperty("total_connections_received", "0")));//获取 Redis 服务器启动以来接收的总连接数
            redisInfo.setTotalCommandsProcessed(Long.valueOf(statsInfo.getProperty("total_commands_processed", "0")));//获取 Redis 服务器启动以来处理的总命令数
            redisInfo.setInstantaneousOpsPerSec(Integer.valueOf(statsInfo.getProperty("instantaneous_ops_per_sec", "0")));//获取 Redis 每秒操作次数的瞬时值（OPS - Operations Per Second）
            redisInfo.setKeyspaceHits(Long.valueOf(statsInfo.getProperty("keyspace_hits", "0")));//获取键空间命中的次数（缓存命中次数）
            redisInfo.setKeyspaceMisses(Long.valueOf(statsInfo.getProperty("keyspace_misses", "0")));//获取键空间未命中的次数（缓存未命中次数）

            // 获取数据库键的数量
            redisInfo.setDbSize(connection.dbSize());

            // 获取键详细信息
            redisInfo.setKeyDetails(getKeyDetails());

            return redisInfo;
        }
    }

    /**
     * 获取键详情
     */
    private List<KeyDetailVO> getKeyDetails() {
        // SCAN 游标全量枚举（禁止 KEYS "*"：O(N) 阻塞 Redis 主线程）
        Set<String> keys = redisUtil.scanKeys("*");
        List<KeyDetailVO> keyDetails = new ArrayList<>();

        for (String key : keys) {
            KeyDetailVO detail = new KeyDetailVO();
            detail.setKeyName(key);

            // 获取键的TTL
            Long ttl = stringRedisTemplate.getExpire(key);
            detail.setTtl(ttl != null ? ttl : -1);

            // 获取键的类型
            DataType type = stringRedisTemplate.type(key);
            detail.setType(type.code());

            // 按值类型取大小：非 string 键不能强转为 String（原实现对 hash/list 等
            // 执行 opsForValue().get 返回 null 还可能抛 WRONGTYPE 异常导致接口 500）
            detail.setSize(estimateSize(key, type));

            keyDetails.add(detail);
        }

        return keyDetails;
    }

    /**
     * 按 Redis 数据类型估算键占用大小
     * <ul>
     *   <li>string：值的 UTF-8 字节数</li>
     *   <li>list/set/zset/hash：元素个数（长度），无对应类型时记 0</li>
     * </ul>
     */
    private Long estimateSize(String key, DataType type) {
        try {
            if (DataType.STRING == type) {
                String value = stringRedisTemplate.opsForValue().get(key);
                return value == null ? 0L : (long) value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            }
            if (DataType.LIST == type) {
                Long size = stringRedisTemplate.opsForList().size(key);
                return size == null ? 0L : size;
            }
            if (DataType.SET == type) {
                Long size = stringRedisTemplate.opsForSet().size(key);
                return size == null ? 0L : size;
            }
            if (DataType.ZSET == type) {
                Long size = stringRedisTemplate.opsForZSet().zCard(key);
                return size == null ? 0L : size;
            }
            if (DataType.HASH == type) {
                Long size = stringRedisTemplate.opsForHash().size(key);
                return size == null ? 0L : size;
            }
            return 0L;
        } catch (Exception ex) {
            // 单个键探测失败不影响整页监控数据
            return 0L;
        }
    }
}
