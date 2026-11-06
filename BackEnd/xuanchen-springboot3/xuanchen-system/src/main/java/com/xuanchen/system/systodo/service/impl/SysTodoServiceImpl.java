package com.xuanchen.system.systodo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.systodo.entity.SysTodo;
import com.xuanchen.system.systodo.mapper.SysTodoMapper;
import com.xuanchen.system.systodo.service.ISysTodoService;
import org.springframework.stereotype.Service;

/**
 * Service-->系统待办
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Service
public class SysTodoServiceImpl extends ServiceImpl<SysTodoMapper, SysTodo> implements ISysTodoService {

    @Override
    public IPage<SysTodo> listMine(IPage<SysTodo> page, SysTodo sysTodo) {
        LambdaQueryWrapper<SysTodo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysTodo::getUserName, sysTodo.getUserName());
        if (StringUtil.isNotEmpty(sysTodo.getTitle())) {
            queryWrapper.like(SysTodo::getTitle, sysTodo.getTitle());
        }
        if (sysTodo.getStatus() != null) {
            queryWrapper.eq(SysTodo::getStatus, sysTodo.getStatus());
        }
        // 未完成在前（置顶），同级按优先级升序、创建时间倒序
        queryWrapper.orderByAsc(SysTodo::getStatus)
                .orderByAsc(SysTodo::getPriority)
                .orderByDesc(SysTodo::getCreateTime);
        return this.page(page, queryWrapper);
    }
}
