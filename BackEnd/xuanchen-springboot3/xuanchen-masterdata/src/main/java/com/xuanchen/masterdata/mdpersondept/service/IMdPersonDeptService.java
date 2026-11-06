package com.xuanchen.masterdata.mdpersondept.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.masterdata.mdpersondept.entity.MdPersonDept;

/**
 * Service接口-->用户 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
public interface IMdPersonDeptService extends IService<MdPersonDept> {
    IPage<MdPersonDept> listDeptUser(Page<MdPersonDept> page, String deptCode);

    Boolean add(String personId, String[] deptIds);
}
