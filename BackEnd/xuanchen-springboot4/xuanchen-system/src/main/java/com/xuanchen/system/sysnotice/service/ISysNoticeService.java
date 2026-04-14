package com.xuanchen.system.sysnotice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysnotice.entity.SysNotice;

/**
 * Service --> 通知
 *
 * @author XuanChen
 * @date 2025-10-21
 */
public interface ISysNoticeService extends IService<SysNotice> {
    /**
     * 分页列表(用户)
     *
     * @param page
     * @param sysNotice
     * @return
     */
    IPage<SysNotice> listUser(Page<SysNotice> page, SysNotice sysNotice);

    /**
     * 分页列表(管理)
     *
     * @param page
     * @param sysNotice
     * @return
     */
    IPage<SysNotice> listManage(Page<SysNotice> page, SysNotice sysNotice);
}
