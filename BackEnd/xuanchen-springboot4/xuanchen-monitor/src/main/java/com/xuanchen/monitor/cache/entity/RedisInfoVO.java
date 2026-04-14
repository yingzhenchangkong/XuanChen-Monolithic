package com.xuanchen.monitor.cache.entity;

import lombok.Data;

import java.util.List;

/**
 * 实体类-->Redis监控键
 *
 * @author XuanChen
 * @date 2026-01-30
 */
@Data
public class RedisInfoVO {
    // 服务器信息
    /**
     * 版本信息字段，用于存储当前实例的版本号
     */
    private String version;
    /**
     * 模式标识字段，用于存储当前实例的运行模式或状态
     */
    private String mode;
    /**
     * 操作系统信息
     * 用于存储当前实例运行所在的操作系统信息
     */
    private String os;
    /**
     * 架构位数
     * 用于存储当前实例的CPU架构位数，通常为32位或64位
     */
    private String archBits;
    /**
     * 进程ID字段，用于存储当前实例的进程ID
     */
    private String processId;

    // 内存信息
    /**
     * 记录当前已使用的内存大小
     * 该变量用于跟踪系统或应用程序当前占用的内存量
     */
    private Long usedMemory;
    /**
     * 存储已使用内存的人类可读格式字符串
     * 该字段用于保存内存使用量的格式化表示，便于用户理解和显示
     */
    private String usedMemoryHuman;
    /**
     * 记录当前进程使用的RSS（Resident Set Size）内存大小
     * RSS表示进程在物理内存中实际占用的内存量（包括共享库等）
     */
    private Long usedMemoryRss;
    /**
     * 最大内存配置值
     * 用于存储系统或应用的最大内存限制，单位通常为字节
     */
    private Long maxMemory;
    /**
     * 内存使用峰值
     * 用于保存内存使用量的峰值，即当前使用内存中最大值
     */
    private Long usedMemoryPeak;
    /**
     * 内存使用峰值的的人类可读格式字符串
     * 该字段用于保存内存使用量的峰值的格式化表示，便于用户理解和显示
     */
    private String usedMemoryPeakHuman;
    /**
     * Lua引擎使用的内存
     * 用于保存Lua引擎使用的内存大小，单位通常为字节
     */
    private Long usedMemoryLua;
    /**
     * Lua内存的的人类可读格式字符串
     * 该字段用于保存Lua内存的格式化表示，便于用户理解和显示
     */
    private String usedMemoryLuaHuman;

    // CPU信息
    /**
     * 系统使用的CPU使用率
     * 该变量记录了系统层面的CPU使用百分比，以小数形式存储（如0.8表示80%）
     */
    private Double usedCpuSys;
    /**
     * 记录用户态CPU使用率的数值
     * 该字段存储当前系统或进程在用户态模式下消耗的CPU时间百分比
     */
    private Double usedCpuUser;
    /**
     * 记录子进程系统CPU使用率的字段
     * 该变量存储了当前进程的子进程在系统模式下使用的CPU时间百分比
     */
    private Double usedCpuSysChildren;
    /**
     * 存储子进程用户态CPU使用率的数值
     * 该变量记录了当前进程的子进程在用户态模式下消耗的CPU时间百分比
     */
    private Double usedCpuUserChildren;

    // 客户端信息
    /**
     * 记录当前连接的客户端数量
     * 用于跟踪系统中活跃的客户端连接数
     */
    private Integer connectedClients;
    /**
     * 最大客户端连接数
     * 用于限制系统同时处理的最大客户端连接数量
     */
    private Integer maxClients;


    // 统计信息
    /**
     * 记录接收到的总连接数
     * 用于统计服务器接收的客户端连接总数
     */
    private Long totalConnectionsReceived;
    /**
     * 记录已处理命令总数的计数器
     * 用于统计系统中已经成功处理的命令数量
     */
    private Long totalCommandsProcessed;
    /**
     * 瞬时操作数每秒的计数值
     * 用于记录系统在某一时刻的操作处理速率，单位为ops/s（operations per second）
     */
    private Integer instantaneousOpsPerSec;
    /**
     * 缓存命中次数统计
     * 记录keyspace中成功命中的查询次数
     */
    private Long keyspaceHits;
    /**
     * 记录keyspace misses的数量
     * keyspace misses表示在Redis中查询不存在的key的次数
     */
    private Long keyspaceMisses;

    // 数据库信息
    /**
     * 数据库大小，用于存储数据库的容量信息
     */
    private Long dbSize;

    // 键详情
    /**
     * 存储密钥详情信息的列表
     * 该字段用于保存KeyDetailVO对象的集合，包含密钥的详细配置和状态信息
     */
    private List<KeyDetailVO> keyDetails;
}
