package com.xuanchen.system.sysrole.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysrole.entity.SysRole;

/**
 * Service接口-->角色
 *
 * @author XuanChen
 * @date 2025-04-06
 */
public interface ISysRoleService extends IService<SysRole> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysRole
     * @return
     */
    IPage<SysRole> listRecycleBin(Page<SysRole> page, SysRole sysRole);

    /**
     * 回收站 彻底删除
     *
     * @param ids
     */
    void deleteRecycleBin(String ids);

    /**
     * 回收站 还原
     *
     * @param ids
     */
    void revertRecycleBin(String ids);
}
