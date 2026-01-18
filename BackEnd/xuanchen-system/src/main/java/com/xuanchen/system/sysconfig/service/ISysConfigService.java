package com.xuanchen.system.sysconfig.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysconfig.entity.SysConfig;

/**
 * Service接口-->参数配置
 *
 * @author XuanChen
 * @date 2026-01-10
 */
public interface ISysConfigService extends IService<SysConfig> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysConfig
     * @return
     */
    IPage<SysConfig> listRecycleBin(Page<SysConfig> page, SysConfig sysConfig);

    /**
     * 回收站 彻底删除
     *
     * @param ids
     */
    void deleteRecycleBin(String ids);

    /**
     * 回收站 还原
     *
     * @param ids
     */
    void revertRecycleBin(String ids);

    Boolean ifExistsId(SysConfig sysConfig);

    Boolean ifExistsNoId(SysConfig sysConfig);
}
