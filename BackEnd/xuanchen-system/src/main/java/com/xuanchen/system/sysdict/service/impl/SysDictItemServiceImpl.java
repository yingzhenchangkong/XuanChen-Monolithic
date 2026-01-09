package com.xuanchen.system.sysdict.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysdict.entity.SysDictItem;
import com.xuanchen.system.sysdict.mapper.SysDictItemMapper;
import com.xuanchen.system.sysdict.service.ISysDictItemService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->字典子表
 *
 * @author XuanChen
 * @date 2025-06-03
 */
@Service
public class SysDictItemServiceImpl extends ServiceImpl<SysDictItemMapper, SysDictItem> implements ISysDictItemService {
}