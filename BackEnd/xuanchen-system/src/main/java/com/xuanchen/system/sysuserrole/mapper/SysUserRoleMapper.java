package com.xuanchen.system.sysuserrole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysuserrole.entity.SysUserRole;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->用户 角色 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@Repository
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
    List<SysUserRole> listAuthUser(@Param("page") Page<SysUserRole> page, @Param("roleId") String roleId);

    List<SysUserRole> listUnAuthUser(@Param("page") Page<SysUserRole> page, @Param("roleId") String roleId);
}