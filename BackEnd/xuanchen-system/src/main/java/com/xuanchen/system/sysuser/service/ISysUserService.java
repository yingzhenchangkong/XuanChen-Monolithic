package com.xuanchen.system.sysuser.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysuser.entity.SysUser;

/**
 * Service接口-->用户
 *
 * @author XuanChen
 * @date 2025-04-03
 */
public interface ISysUserService extends IService<SysUser> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysUser
     * @return
     */
    IPage<SysUser> listRecycleBin(Page<SysUser> page, SysUser sysUser);

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

    Boolean ifExistsId(SysUser sysUser);

    Boolean ifExistsNoId(SysUser sysUser);
}
