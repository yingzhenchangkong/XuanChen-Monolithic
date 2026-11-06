package com.xuanchen.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.api.entity.LogApi;

/**
 * Service接口-->数据接口服务-调用日志
 *
 * @author XuanChen
 * @date 2026-10-28
 */
public interface ILogApiService extends IService<LogApi> {

    /**
     * 分页查询调用日志（按调用时间倒序）。
     *
     * @param query    查询条件（apiCode 模糊、triggerType/success 精确、beginTime~endTime 区间）
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果（apiName 已按 api_code 回填）
     */
    IPage<LogApi> pageQuery(LogApi query, Integer pageNo, Integer pageSize);
}
