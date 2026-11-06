package com.xuanchen.masterdata.mdpersondept.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.masterdata.mdpersondept.entity.MdPersonDept;
import com.xuanchen.masterdata.mdpersondept.mapper.MdPersonDeptMapper;
import com.xuanchen.masterdata.mdpersondept.service.IMdPersonDeptService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class MdPersonDeptServiceImpl extends ServiceImpl<MdPersonDeptMapper, MdPersonDept> implements IMdPersonDeptService {
    private final MdPersonDeptMapper mdPersonDeptMapper;

    @Override
    public IPage<MdPersonDept> listDeptUser(Page<MdPersonDept> page, String deptCode) {
        return page.setRecords(mdPersonDeptMapper.listDeptUser(page, deptCode));
    }

    @Override
    public Boolean add(String personId, String[] deptIds) {
        if (deptIds == null) {
            return false;
        }
        List<MdPersonDept> list = new ArrayList<>();
        for (String deptId : deptIds) {
            MdPersonDept mdPersonDept = new MdPersonDept();
            mdPersonDept.setPersonId(personId);
            mdPersonDept.setDeptId(deptId);
            list.add(mdPersonDept);
        }
        return saveBatch(list);
    }
}
