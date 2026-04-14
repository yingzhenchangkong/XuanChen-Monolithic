package com.xuanchen.system.sysuserdept.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysuserdept.entity.SysUserDept;
import com.xuanchen.system.sysuserdept.mapper.SysUserDeptMapper;
import com.xuanchen.system.sysuserdept.service.ISysUserDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service接口实现类-->用户 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
@Service
public class SysUserDeptServiceImpl extends ServiceImpl<SysUserDeptMapper, SysUserDept> implements ISysUserDeptService {
    @Autowired
    private SysUserDeptMapper sysUserDeptMapper;

    @Override
    public IPage<SysUserDept> listDeptUser(Page<SysUserDept> page, String deptCode) {
        return page.setRecords(sysUserDeptMapper.listDeptUser(page, deptCode));
    }

    @Override
    public Boolean add(String userId, String[] deptIds) {
        if (deptIds == null) {
            return false;
        }
        List<SysUserDept> list = new ArrayList<>();
        for (String deptId : deptIds) {
            SysUserDept sysUserDept = new SysUserDept();
            sysUserDept.setUserId(userId);
            sysUserDept.setDeptId(deptId);
            list.add(sysUserDept);
        }
        return saveBatch(list);
    }
}
