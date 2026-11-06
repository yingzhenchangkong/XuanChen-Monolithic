package com.xuanchen.businessprocess.support.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.businessprocess.support.entity.BpmDelegate;

import java.util.List;

/**
 * Service-->审批委托/代理
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface IBpmDelegateService extends IService<BpmDelegate> {

    /**
     * 我的委托规则（按创建时间倒序）
     */
    List<BpmDelegate> listMine(String userName);
}
