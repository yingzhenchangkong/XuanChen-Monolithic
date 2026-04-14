package com.xuanchen.system.sysconfig.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysconfig.entity.SysConfig;
import com.xuanchen.system.sysconfig.mapper.SysConfigMapper;
import com.xuanchen.system.sysconfig.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->参数配置
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {
    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Override
    public IPage<SysConfig> listRecycleBin(Page<SysConfig> page, SysConfig sysConfig) {
        return page.setRecords(sysConfigMapper.listRecycleBin(page, sysConfig));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysConfigMapper.deleteRecycleBin(idArray);
    }

    @Override
    public void revertRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysConfigMapper.revertRecycleBin(idArray);
    }

    @Override
    public Boolean ifExistsId(SysConfig sysConfig) {
        return sysConfigMapper.ifExistsId(sysConfig).size() > 0;
    }

    @Override
    public Boolean ifExistsNoId(SysConfig sysConfig) {
        return sysConfigMapper.ifExistsNoId(sysConfig).size() > 0;
    }
}