package com.xuanchen.system.sysaccountrole.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.system.sysaccountrole.entity.SysAccountRole;
import com.xuanchen.system.sysaccountrole.mapper.SysAccountRoleMapper;
import com.xuanchen.system.sysaccountrole.service.ISysAccountRoleService;
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
public class SysAccountRoleServiceImpl extends ServiceImpl<SysAccountRoleMapper, SysAccountRole> implements ISysAccountRoleService {
    @Autowired
    private SysAccountRoleMapper sysAccountRoleMapper;

    @Override
    public IPage<SysAccountRole> listAuthUser(Page<SysAccountRole> page, String roleId) {
        return page.setRecords(sysAccountRoleMapper.listAuthUser(page, roleId));
    }

    @Override
    public IPage<SysAccountRole> listUnAuthUser(Page<SysAccountRole> page, String roleId) {
        return page.setRecords(sysAccountRoleMapper.listUnAuthUser(page, roleId));
    }

    /**
     * 添加 用户 角色 对应关系
     *
     * @param accountId
     * @param roleIds
     * @return
     */
    @Override
    public Boolean add(String accountId, String[] roleIds) {
        if (roleIds == null) {
            return false;
        }
        List<SysAccountRole> list = new ArrayList<>();
        for (String roleId : roleIds) {
            SysAccountRole sysAccountRole = new SysAccountRole();
            sysAccountRole.setAccountId(accountId);
            sysAccountRole.setRoleId(roleId);
            list.add(sysAccountRole);
        }
        return saveBatch(list);
    }
}