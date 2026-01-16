package com.xuanchen.system.sysrole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysrole.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->角色
 *
 * @author XuanChen
 * @date 2025-04-06
 */
@Repository
public interface SysRoleMapper extends BaseMapper<SysRole> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysRole
     * @return
     */
    List<SysRole> listRecycleBin(@Param("page") Page<SysRole> page, @Param("sysRole") SysRole sysRole);

    /**
     * 回收站 彻底删除
     *
     * @param sysRoleIds
     * @return
     */
    Integer deleteRecycleBin(@Param("sysRoleIds") String[] sysRoleIds);

    /**
     * 回收站 彻底删除 用户 角色 关系
     *
     * @param sysRoleIds
     * @return
     */
    Integer deleteUserRole(@Param("sysRoleIds") String[] sysRoleIds);

    /**
     * 回收站 彻底删除 角色 权限 关系
     *
     * @param sysRoleIds
     * @return
     */
    Integer deleteRoleMenu(@Param("sysRoleIds") String[] sysRoleIds);

    /**
     * 回收站 还原
     *
     * @param sysRoleIds
     * @return
     */
    Integer revertRecycleBin(@Param("sysRoleIds") String[] sysRoleIds);

    List<SysRole> validate(@Param("sysRole") SysRole sysRole);
}
