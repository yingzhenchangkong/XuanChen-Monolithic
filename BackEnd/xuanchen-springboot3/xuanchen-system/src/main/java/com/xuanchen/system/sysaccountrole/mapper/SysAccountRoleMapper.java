package com.xuanchen.system.sysaccountrole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysaccountrole.entity.SysAccountRole;
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
public interface SysAccountRoleMapper extends BaseMapper<SysAccountRole> {
    List<SysAccountRole> listAuthUser(@Param("page") Page<SysAccountRole> page, @Param("roleId") String roleId);

    List<SysAccountRole> listUnAuthUser(@Param("page") Page<SysAccountRole> page, @Param("roleId") String roleId);
}