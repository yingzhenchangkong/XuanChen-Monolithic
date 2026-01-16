package com.xuanchen.system.syspost.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.syspost.entity.SysPost;
import com.xuanchen.system.syspost.mapper.SysPostMapper;
import com.xuanchen.system.syspost.service.ISysPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->岗位
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements ISysPostService {
    @Autowired
    private SysPostMapper sysPostMapper;

    @Override
    public IPage<SysPost> listRecycleBin(Page<SysPost> page, SysPost sysPost) {
        return page.setRecords(sysPostMapper.listRecycleBin(page, sysPost));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysPostMapper.deleteRecycleBin(idArray);
    }

    @Override
    public void revertRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysPostMapper.revertRecycleBin(idArray);
    }

    @Override
    public Boolean validate(SysPost sysPost) {
        return sysPostMapper.validate(sysPost).size() > 0;
    }
}
