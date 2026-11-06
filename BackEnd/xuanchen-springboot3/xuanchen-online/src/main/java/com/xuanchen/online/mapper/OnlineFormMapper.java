package com.xuanchen.online.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.online.entity.OnlineForm;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper接口-->在线表单（流程审批）
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Mapper
public interface OnlineFormMapper extends BaseMapper<OnlineForm> {
}
