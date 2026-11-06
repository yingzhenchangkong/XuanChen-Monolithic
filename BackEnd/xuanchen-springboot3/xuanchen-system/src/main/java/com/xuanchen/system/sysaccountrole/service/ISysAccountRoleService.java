package com.xuanchen.system.sysaccountrole.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.system.sysaccountrole.entity.SysAccountRole;

/**
 * Service接口-->用户 角色 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
public interface ISysAccountRoleService extends IService<SysAccountRole> {
    IPage<SysAccountRole> listAuthUser(Page<SysAccountRole> page, String roleId);

    IPage<SysAccountRole> listUnAuthUser(Page<SysAccountRole> page, String roleId);

    Boolean add(String accountId, String[] roleIds);
}