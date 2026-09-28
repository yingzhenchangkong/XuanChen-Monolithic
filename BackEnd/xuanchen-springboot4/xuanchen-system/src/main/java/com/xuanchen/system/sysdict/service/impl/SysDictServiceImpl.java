package com.xuanchen.system.sysdict.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.system.sysdict.entity.SysDict;
import com.xuanchen.system.sysdict.mapper.SysDictMapper;
import com.xuanchen.system.sysdict.service.ISysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->字典主表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements ISysDictService {
    private final SysDictMapper sysDictMapper;

    @Override
    public Boolean ifExistsId(SysDict sysDict) {
        return sysDictMapper.ifExistsId(sysDict).size() > 0;
    }

    @Override
    public Boolean ifExistsNoId(SysDict sysDict) {
        return sysDictMapper.ifExistsNoId(sysDict).size() > 0;
    }
}