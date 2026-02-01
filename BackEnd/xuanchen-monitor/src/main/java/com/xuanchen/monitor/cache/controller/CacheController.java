package com.xuanchen.monitor.cache.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.monitor.cache.entity.KeyDetailVO;
import com.xuanchen.monitor.cache.entity.RedisInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
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
public class CacheController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/redis")
    public Result getRedisInfo() {
        RedisInfoVO redisInfo = getRedisDetailedInfo();
        return Result.success(redisInfo);
    }

    /**
     * 获取Redis详细信息
     */
    public RedisInfoVO getRedisDetailedInfo() {
        RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();

        // 获取INFO信息
        Properties info = connection.info();

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
        redisInfo.setUsedCpuUserChildren(Double.valueOf(cpuInfo.getProperty("used_cpu_user_children", "0")));//Redis 后台子进程在内核态的 CPU 时间占比

        // 设置客户端信息
        redisInfo.setConnectedClients(Integer.valueOf(clientsInfo.getProperty("connected_clients", "0")));//获取当前已连接到 Redis 服务器的客户端数量；这个值表示当前活跃的连接数
        redisInfo.setMaxClients(Integer.valueOf(clientsInfo.getProperty("maxclients", "0")));//获取 Redis 服务器配置的最大客户端连接数限制；这是 Redis 服务端设置的最大并发连接数上限

        // 设置统计信息
        redisInfo.setTotalConnectionsReceived(Long.valueOf(statsInfo.getProperty("total_connections_received", "0")));//获取 Redis 服务器启动以来接收的总连接数
        redisInfo.setTotalCommandsProcessed(Long.valueOf(statsInfo.getProperty("total_commands_processed", "0")));//获取 Redis 服务器启动以来处理的总命令数
        redisInfo.setInstantaneousOpsPerSec(Integer.valueOf(statsInfo.getProperty("instantaneous_ops_per_sec", "0")));//获取 Redis 每秒操作次数的瞬时值（OPS - Operations Per Second）
        redisInfo.setKeyspaceHits(Long.valueOf(statsInfo.getProperty("keyspace_hits", "0")));//获取键空间命中的次数（缓存命中次数）
        redisInfo.setKeyspaceMisses(Long.valueOf(statsInfo.getProperty("keyspace_misses", "0")));//获取键空间未命中的次数（缓存未命中次数）

        // 获取数据库键的数量
        redisInfo.setDbSize(getDatabaseSize(connection));

        // 获取键详细信息
        redisInfo.setKeyDetails(getKeyDetails());

        return redisInfo;
    }

    /**
     * 获取数据库键数量
     */
    private Long getDatabaseSize(RedisConnection connection) {
        return connection.dbSize();
    }

    /**
     * 获取键详情
     */
    private List<KeyDetailVO> getKeyDetails() {
        Set<String> keys = stringRedisTemplate.keys("*");
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

            // 获取键的大小（估算）
            String value = (String) stringRedisTemplate.opsForValue().get(key);
            detail.setSize(value != null ? value.getBytes().length : 0);

            keyDetails.add(detail);
        }

        return keyDetails;
    }
}
