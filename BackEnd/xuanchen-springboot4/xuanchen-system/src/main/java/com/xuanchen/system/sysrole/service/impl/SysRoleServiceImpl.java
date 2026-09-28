package com.xuanchen.system.sysrole.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.system.sysrole.entity.SysRole;
import com.xuanchen.system.sysrole.mapper.SysRoleMapper;
import com.xuanchen.system.sysrole.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service接口实现类-->角色
 *
 * @author XuanChen
 * @date 2025-04-06
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    private final SysRoleMapper sysRoleMapper;

    @Override
    public IPage<SysRole> listRecycleBin(Page<SysRole> page, SysRole sysRole) {
        return page.setRecords(sysRoleMapper.listRecycleBin(page, sysRole));
    }

    /**
     * 回收站彻底删除：角色主表与用户-角色、角色-菜单关系必须同一事务，
     * 中途异常整体回滚，避免角色已删但授权关系残留。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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
    public Boolean ifExistsId(SysRole sysRole) {
        return sysRoleMapper.ifExistsId(sysRole).size() > 0;
    }

    @Override
    public Boolean ifExistsNoId(SysRole sysRole) {
        return sysRoleMapper.ifExistsNoId(sysRole).size() > 0;
    }
}
