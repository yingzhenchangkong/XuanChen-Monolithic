package com.xuanchen.system.sysnotice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysnotice.entity.SysNoticeStatus;

/**
 * Service --> 通知 阅读状态
 *
 * @author XuanChen
 * @date 2025-10-21
 */
public interface ISysNoticeStatusService extends IService<SysNoticeStatus> {
    /**
     * 分页列表
     *
     * @param page
     * @param sysNoticeStatus
     * @return
     */
    IPage<SysNoticeStatus> list(Page<SysNoticeStatus> page, SysNoticeStatus sysNoticeStatus);

    /**
     * 获取用户未读通知数量
     *
     * @param userId
     * @return
     */
    Integer getNoticeCountByUserName(String userId);
}
