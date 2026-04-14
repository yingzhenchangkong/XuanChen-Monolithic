package com.xuanchen.system.sysuserdept.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysuserdept.entity.SysUserDept;

/**
 * Service接口-->用户 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
public interface ISysUserDeptService extends IService<SysUserDept> {
    IPage<SysUserDept> listDeptUser(Page<SysUserDept> page, String deptCode);

    Boolean add(String userId, String[] deptIds);
}
