package com.xuanchen.system.sysdict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysdict.entity.SysDictItem;

/**
 * Service接口-->字典子表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
public interface ISysDictItemService extends IService<SysDictItem> {
    Boolean ifExistsId(SysDictItem sysDictItem);

    Boolean ifExistsNoId(SysDictItem sysDictItem);
}