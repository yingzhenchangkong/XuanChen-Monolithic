package com.xuanchen.system.sysdatasource.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.system.sysdatasource.entity.SysDatasource;

/**
 * Service接口-->报表数据源
 *
 * @author XuanChen
 * @date 2026-10-22
 */
public interface ISysDatasourceService extends IService<SysDatasource> {

    /**
     * 测试数据源连接
     *
     * @param datasource 含 jdbcUrl/username/password（password 为 AES 密文）
     * @return 连接成功返回 null，失败返回错误信息
     */
    String testConnection(SysDatasource datasource);
}
