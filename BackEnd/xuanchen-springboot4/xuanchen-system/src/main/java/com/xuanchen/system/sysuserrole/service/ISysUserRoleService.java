package com.xuanchen.system.sysuserrole.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysuserrole.entity.SysUserRole;

/**
 * Service接口-->用户 角色 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
public interface ISysUserRoleService extends IService<SysUserRole> {
    IPage<SysUserRole> listAuthUser(Page<SysUserRole> page, String roleId);

    IPage<SysUserRole> listUnAuthUser(Page<SysUserRole> page, String roleId);

    Boolean add(String userId, String[] roleIds);
}