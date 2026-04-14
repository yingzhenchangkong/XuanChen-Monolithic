package com.xuanchen.system.sysuserrole.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysuserrole.entity.SysUserRole;
import com.xuanchen.system.sysuserrole.mapper.SysUserRoleMapper;
import com.xuanchen.system.sysuserrole.service.ISysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service接口实现类-->用户 角色 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements ISysUserRoleService {
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public IPage<SysUserRole> listAuthUser(Page<SysUserRole> page, String roleId) {
        return page.setRecords(sysUserRoleMapper.listAuthUser(page, roleId));
    }

    @Override
    public IPage<SysUserRole> listUnAuthUser(Page<SysUserRole> page, String roleId) {
        return page.setRecords(sysUserRoleMapper.listUnAuthUser(page, roleId));
    }

    /**
     * 添加 用户 角色 对应关系
     *
     * @param userId
     * @param roleIds
     * @return
     */
    @Override
    public Boolean add(String userId, String[] roleIds) {
        if (roleIds == null) {
            return false;
        }
        List<SysUserRole> list = new ArrayList<>();
        for (String roleId : roleIds) {
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(userId);
            sysUserRole.setRoleId(roleId);
            list.add(sysUserRole);
        }
        return saveBatch(list);
    }
}