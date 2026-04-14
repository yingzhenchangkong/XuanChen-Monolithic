package com.xuanchen.system.sysuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysuser.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->用户
 *
 * @author XuanChen
 * @date 2025-04-03
 */
@Repository
public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysUser
     * @return
     */
    List<SysUser> listRecycleBin(@Param("page") Page<SysUser> page, @Param("sysUser") SysUser sysUser);

    /**
     * 回收站 彻底删除
     *
     * @param sysUserIds
     * @return
     */
    Integer deleteRecycleBin(@Param("sysUserIds") String[] sysUserIds);

    /**
     * 回收站 彻底删除 用户 角色 关系
     *
     * @param sysUserIds
     * @return
     */
    Integer deleteUserRole(@Param("sysUserIds") String[] sysUserIds);

    /**
     * 回收站 还原
     *
     * @param sysUserIds
     * @return
     */
    Integer revertRecycleBin(@Param("sysUserIds") String[] sysUserIds);

    List<SysUser> ifExistsId(@Param("sysUser") SysUser sysUser);

    List<SysUser> ifExistsNoId(@Param("sysUser") SysUser sysUser);
}
