package com.xuanchen.system.sysdept.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysdept.entity.SysDeptTreeVO;
import com.xuanchen.system.sysdept.entity.SysDept;

import java.util.List;

/**
 * Service --> 部门
 *
 * @author XuanChen
 * @date 2025-11-12
 */
public interface ISysDeptService extends IService<SysDept> {
    List<SysDeptTreeVO> getDeptTree();

    List<SysDeptTreeVO> getChildDeptTree(String parentDeptCode);

    String createDeptCode(String parentDeptCode);
}
