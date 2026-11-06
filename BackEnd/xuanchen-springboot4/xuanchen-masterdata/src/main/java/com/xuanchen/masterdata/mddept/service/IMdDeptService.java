package com.xuanchen.masterdata.mddept.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.masterdata.mddept.entity.MdDeptTreeVO;
import com.xuanchen.masterdata.mddept.entity.MdDept;

import java.util.List;

/**
 * Service --> 部门
 *
 * @author XuanChen
 * @date 2025-11-12
 */
public interface IMdDeptService extends IService<MdDept> {
    List<MdDeptTreeVO> getDeptTree();

    List<MdDeptTreeVO> getChildDeptTree(String parentDeptCode);

    String createDeptCode(String parentDeptCode);
}
