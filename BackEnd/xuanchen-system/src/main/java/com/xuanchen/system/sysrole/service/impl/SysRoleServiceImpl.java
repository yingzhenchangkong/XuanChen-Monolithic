package com.xuanchen.system.sysrole.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysrole.entity.SysRole;
import com.xuanchen.system.sysrole.mapper.SysRoleMapper;
import com.xuanchen.system.sysrole.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->角色
 *
 * @author XuanChen
 * @date 2025-04-06
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public IPage<SysRole> listRecycleBin(Page<SysRole> page, SysRole sysRole) {
        return page.setRecords(sysRoleMapper.listRecycleBin(page, sysRole));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysRoleMapper.deleteRecycleBin(idArray);
        sysRoleMapper.deleteUserRole(idArray);
        sysRoleMapper.deleteRoleMenu(idArray);
    }

    @Override
    public void revertRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysRoleMapper.revertRecycleBin(idArray);
    }

    @Override
    public Boolean validate(SysRole sysRole) {
        return sysRoleMapper.validate(sysRole).size() > 0;
    }
}
