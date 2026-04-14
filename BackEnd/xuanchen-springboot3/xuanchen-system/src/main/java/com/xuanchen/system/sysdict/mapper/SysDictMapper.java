package com.xuanchen.system.sysdict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.system.sysdict.entity.SysDict;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->字典主表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
@Repository
public interface SysDictMapper extends BaseMapper<SysDict> {
    List<SysDict> ifExistsId(@Param("sysDict") SysDict sysDict);

    List<SysDict> ifExistsNoId(@Param("sysDict") SysDict sysDict);
}