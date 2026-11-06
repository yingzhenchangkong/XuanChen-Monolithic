package com.xuanchen.businessprocess.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.businessprocess.support.entity.BpmDelegate;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper-->审批委托/代理
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Mapper
public interface BpmDelegateMapper extends BaseMapper<BpmDelegate> {
}
