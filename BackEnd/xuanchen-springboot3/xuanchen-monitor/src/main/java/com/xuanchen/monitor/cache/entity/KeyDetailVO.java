package com.xuanchen.monitor.cache.entity;

import lombok.Data;
/**
 * 实体类-->Redis监控键明细
 *
 * @author XuanChen
 * @date 2026-01-30
 */
@Data
public class KeyDetailVO {
    /**
     * 存储键名的私有字符串字段
     */
    private String keyName;
    /**
     * 表示对象类型的私有字段
     */
    private String type;
    /**
     * TTL (Time To Live) 值，表示对象的有效期时间
     * 通常用于缓存、会话或临时数据的过期控制
     */
    private Long ttl;
    /**
     * 存储对象的大小信息
     * 用于记录当前实例的尺寸或容量值
     */
    private Integer size;
}
