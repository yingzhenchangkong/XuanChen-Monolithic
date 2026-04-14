package com.xuanchen.system.sysdict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysdict.entity.SysDict;

/**
 * Service接口-->字典主表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
public interface ISysDictService extends IService<SysDict> {
    Boolean ifExistsId(SysDict sysDict);

    Boolean ifExistsNoId(SysDict sysDict);
}