package com.xuanchen.system.syspost.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.syspost.entity.SysPost;

/**
 * Service接口-->岗位
 *
 * @author XuanChen
 * @date 2026-01-10
 */
public interface ISysPostService extends IService<SysPost> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysPost
     * @return
     */
    IPage<SysPost> listRecycleBin(Page<SysPost> page, SysPost sysPost);

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

    Boolean ifExistsId(SysPost sysPost);

    Boolean ifExistsNoId(SysPost sysPost);
}
