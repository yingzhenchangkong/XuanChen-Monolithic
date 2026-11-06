package com.xuanchen.system.sysaccount.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.system.sysaccount.entity.SysAccountView;
import com.xuanchen.system.sysaccount.entity.SysAccount;

/**
 * Service接口-->用户
 *
 * @author XuanChen
 * @date 2025-04-03
 */
public interface ISysAccountService extends IService<SysAccount> {
    /**
     * 分页列表（JOIN 人员信息表）
     */
    IPage<SysAccountView> listWithPerson(Page<SysAccountView> page, SysAccountView sysAccountView);

    /**
     * 全量查询（JOIN 人员信息表）
     */
    java.util.List<SysAccountView> listAllWithPerson(SysAccountView sysAccountView);

    /**
     * 回收站 列表
     */
    IPage<SysAccountView> listRecycleBin(Page<SysAccountView> page, SysAccountView sysAccountView);

    /**
     * 回收站 彻底删除
     */
    void deleteRecycleBin(String ids);

    /**
     * 回收站 还原
     */
    void revertRecycleBin(String ids);

    Boolean ifExistsId(SysAccountView sysAccountView);

    Boolean ifExistsNoId(SysAccountView sysAccountView);
}
