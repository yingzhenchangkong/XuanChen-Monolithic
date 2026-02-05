package com.xuanchen.monitor.server.entity;

import lombok.Data;

/**
 * 实体类-->性能监控 服务器信息 磁盘信息
 *
 * @author XuanChen
 * @date 2026-02-02
 */
@Data
public class DiskInfoVO {
    /**
     * 磁盘基本信息
     */
    private String name;              // 磁盘名称
    private String path;              // 磁盘路径
    private String type;              // 磁盘类型

    /**
     * 磁盘空间信息
     */
    private Long total;               // 总容量(G)
    private Long used;                // 已用容量(G)
    private Long free;                // 剩余容量(G)
    private String usage;             // 使用率

    /**
     * 磁盘读写信息
     */
    private Long read;                // 读取字节数
    private Long write;               // 写入字节数
    private Long readCount;           // 读取次数
    private Long writeCount;          // 写入次数
}
