package com.xuanchen.system.sysuser.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.system.sysuser.entity.SysUser;
import com.xuanchen.system.sysuser.mapper.SysUserMapper;
import com.xuanchen.system.sysuser.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service接口实现类-->用户
 *
 * @author XuanChen
 * @date 2025-04-03
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    private final SysUserMapper sysUserMapper;

    @Override
    public IPage<SysUser> listRecycleBin(Page<SysUser> page, SysUser sysUser) {
        return page.setRecords(sysUserMapper.listRecycleBin(page, sysUser));
    }

    /**
     * 回收站彻底删除：主表与全部关系表（角色/部门/岗位）必须同一事务，
     * 任一步失败整体回滚，避免只删了部分关系产生孤儿数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysUserMapper.deleteRecycleBin(idArray);
        sysUserMapper.deleteUserRole(idArray);
        sysUserMapper.deleteUserDept(idArray);
        sysUserMapper.deleteUserPost(idArray);
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
