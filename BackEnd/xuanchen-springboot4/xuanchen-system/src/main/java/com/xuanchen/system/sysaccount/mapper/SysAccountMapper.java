package com.xuanchen.system.sysaccount.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysaccount.entity.SysAccountView;
import com.xuanchen.system.sysaccount.entity.SysAccount;
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
public interface SysAccountMapper extends BaseMapper<SysAccount> {
    /**
     * 分页列表（账号表 JOIN 人员信息表）
     */
    List<SysAccountView> listWithPerson(@Param("page") Page<SysAccountView> page, @Param("sysAccountView") SysAccountView sysAccountView);

    /**
     * 全量查询（账号表 JOIN 人员信息表），用于下拉框、导出等场景
     */
    List<SysAccountView> listAllWithPerson(@Param("sysAccountView") SysAccountView sysAccountView);

    /**
     * 回收站 列表
     */
    List<SysAccountView> listRecycleBin(@Param("page") Page<SysAccountView> page, @Param("sysAccountView") SysAccountView sysAccountView);

    /**
     * 回收站 彻底删除 账号表
     */
    Integer deleteRecycleBin(@Param("sysUserIds") String[] sysUserIds);


    /**
     * 回收站 彻底删除 用户 角色 关系
     */
    Integer deleteUserRole(@Param("sysUserIds") String[] sysUserIds);



    /**
     * 回收站 还原
     */
    Integer revertRecycleBin(@Param("sysUserIds") String[] sysUserIds);

    List<SysAccountView> ifExistsId(@Param("sysAccountView") SysAccountView sysAccountView);

    List<SysAccountView> ifExistsNoId(@Param("sysAccountView") SysAccountView sysAccountView);
}
