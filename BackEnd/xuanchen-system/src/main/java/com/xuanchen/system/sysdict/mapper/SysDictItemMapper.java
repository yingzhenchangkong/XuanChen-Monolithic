package com.xuanchen.system.sysdict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.system.sysdict.entity.SysDictItem;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->字典子表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
@Repository
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {
    List<SysDictItem> ifExistsId(@Param("sysDictItem") SysDictItem sysDictItem);

    List<SysDictItem> ifExistsNoId(@Param("sysDictItem") SysDictItem sysDictItem);
}