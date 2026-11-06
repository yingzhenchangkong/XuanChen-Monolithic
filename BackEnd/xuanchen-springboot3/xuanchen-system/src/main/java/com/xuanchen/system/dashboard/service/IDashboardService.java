package com.xuanchen.system.dashboard.service;

import com.xuanchen.system.dashboard.vo.LoginTrendVO;

import java.util.List;
import java.util.Map;

/**
 * Service接口-->工作台聚合数据
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface IDashboardService {

    /**
     * 工作台汇总：未读公告数、待办数、今日登录数、未读公告 top5、待办 top5
     *
     * @param userName 登录名
     * @return 汇总数据
     */
    Map<String, Object> summary(String userName);

    /**
     * 近 7 日登录趋势（缺日补零）
     *
     * @return 按天升序
     */
    List<LoginTrendVO> loginTrend();
}
