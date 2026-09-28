package com.xuanchen.system.sysnotice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.system.sysnotice.entity.SysNotice;
import com.xuanchen.system.sysnotice.mapper.SysNoticeMapper;
import com.xuanchen.system.sysnotice.service.ISysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ServiceImpl --> 通知
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService {
    private final SysNoticeMapper sysNoticeMapper;

    @Override
    public IPage<SysNotice> listUser(Page<SysNotice> page, SysNotice sysNotice) {
        return page.setRecords(sysNoticeMapper.listUser(page, sysNotice));
    }

    @Override
    public IPage<SysNotice> listManage(Page<SysNotice> page, SysNotice sysNotice) {
        return page.setRecords(sysNoticeMapper.listManage(page, sysNotice));
    }
}
