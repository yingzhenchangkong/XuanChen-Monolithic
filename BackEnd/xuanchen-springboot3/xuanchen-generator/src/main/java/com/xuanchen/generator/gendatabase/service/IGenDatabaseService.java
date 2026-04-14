package com.xuanchen.generator.gendatabase.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;

/**
 * Service接口-->数据库
 *
 * @author XuanChen
 * @date 2026-02-23
 */
public interface IGenDatabaseService extends IService<GenDatabase> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param genDatabase
     * @return
     */
    IPage<GenDatabase> listRecycleBin(Page<GenDatabase> page, GenDatabase genDatabase);

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
}
