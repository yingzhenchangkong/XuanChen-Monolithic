package com.xuanchen.masterdata.mdpersondept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.masterdata.mdpersondept.entity.MdPersonDept;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->用户 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
@Repository
public interface MdPersonDeptMapper extends BaseMapper<MdPersonDept> {
    List<MdPersonDept> listDeptUser(@Param("page") Page<MdPersonDept> page, @Param("deptCode") String deptCode);
}
