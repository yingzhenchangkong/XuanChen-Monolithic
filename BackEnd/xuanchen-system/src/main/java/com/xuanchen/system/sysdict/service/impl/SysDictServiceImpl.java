package com.xuanchen.system.sysdict.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysdict.entity.SysDict;
import com.xuanchen.system.sysdict.mapper.SysDictMapper;
import com.xuanchen.system.sysdict.service.ISysDictService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->字典主表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements ISysDictService {
}