package com.xuanchen.system.sysnotice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysnotice.entity.SysNoticeStatus;
import com.xuanchen.system.sysnotice.mapper.SysNoticeStatusMapper;
import com.xuanchen.system.sysnotice.service.ISysNoticeStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ServiceImpl --> 通知 阅读状态
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Service
public class SysNoticeStatusServiceImpl extends ServiceImpl<SysNoticeStatusMapper, SysNoticeStatus> implements ISysNoticeStatusService {
    @Autowired
    private SysNoticeStatusMapper sysNoticeStatusMapper;

    @Override
    public IPage<SysNoticeStatus> list(Page<SysNoticeStatus> page, SysNoticeStatus sysNoticeStatus) {
        return page.setRecords(sysNoticeStatusMapper.list(page, sysNoticeStatus));
    }

    @Override
    public Integer getNoticeCountByUserName(String userId) {
        return sysNoticeStatusMapper.getNoticeCountByUserName(userId);
    }
}
