package com.xuanchen.system.sysuser.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysuser.entity.SysUser;
import com.xuanchen.system.sysuser.mapper.SysUserMapper;
import com.xuanchen.system.sysuser.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->用户
 *
 * @author XuanChen
 * @date 2025-04-03
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<SysUser> listRecycleBin(Page<SysUser> page, SysUser sysUser) {
        return page.setRecords(sysUserMapper.listRecycleBin(page, sysUser));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysUserMapper.deleteRecycleBin(idArray);
        sysUserMapper.deleteUserRole(idArray);
    }

    @Override
    public void revertRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysUserMapper.revertRecycleBin(idArray);
    }

    @Override
    public Boolean ifExistsId(SysUser sysUser) {
        return sysUserMapper.ifExistsId(sysUser).size() > 0;
    }

    @Override
    public Boolean ifExistsNoId(SysUser sysUser) {
        return sysUserMapper.ifExistsNoId(sysUser).size() > 0;
    }
}
