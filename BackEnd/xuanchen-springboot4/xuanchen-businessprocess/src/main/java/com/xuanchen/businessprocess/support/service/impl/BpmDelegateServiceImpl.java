package com.xuanchen.businessprocess.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.businessprocess.support.entity.BpmDelegate;
import com.xuanchen.businessprocess.support.mapper.BpmDelegateMapper;
import com.xuanchen.businessprocess.support.service.IBpmDelegateService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service-->审批委托/代理
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Service
public class BpmDelegateServiceImpl extends ServiceImpl<BpmDelegateMapper, BpmDelegate>
        implements IBpmDelegateService {

    @Override
    public List<BpmDelegate> listMine(String userName) {
        return this.list(new LambdaQueryWrapper<BpmDelegate>()
                .eq(BpmDelegate::getUserName, userName)
                .orderByDesc(BpmDelegate::getCreateTime));
    }
}
