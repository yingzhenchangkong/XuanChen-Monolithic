package com.xuanchen.monitor.server.entity;

import lombok.Data;

import java.util.List;

/**
 * 实体类-->性能监控 服务器信息
 *
 * @author XuanChen
 * @date 2026-02-02
 */
@Data
public class ServerInfoVO {
    /**
     * CPU相关信息
     */
    private Integer cpuNum;           // CPU核心数
    private String cpuUsed;           // CPU使用率
    private String cpuSys;            // CPU系统使用率
    private String cpuFree;           // CPU剩余率

    /**
     * 内存相关信息
     */
    private Long totalMem;            // 总内存(G)
    private Long usedMem;             // 已用内存(G)
    private Long freeMem;             // 剩余内存(G)
    private String memUsage;          // 内存使用率

    /**
     * JVM相关信息
     */
    private Long jvmTotal;            // JVM总内存(M)
    private Long jvmMax;              // JVM最大内存(M)
    private Long jvmFree;             // JVM空闲内存(M)
    private Long jvmUsed;             // JVM已用内存(M)
    private String jvmUsage;          // JVM使用率
    private String jvmName;           // JVM名称
    private String jvmVersion;        // JVM版本

    /**
     * 系统相关信息
     */
    private String osName;            // 操作系统名称
    private String computerName;      // 服务器名称
    private String userDir;           // 用户目录

    /**
     * 磁盘信息
     */
    private List<DiskInfoVO> diskInfos; // 磁盘信息列表
}
