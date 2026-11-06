package com.xuanchen.system.dashboard.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.dashboard.service.IDashboardService;
import com.xuanchen.system.dashboard.vo.LoginTrendVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 控制器-->工作台聚合数据
 *
 * <p>数据按当前登录用户裁剪：公告/待办为本人数据，登录趋势为全系统口径
 * （与"系统监控→登录日志"一致，普通用户可看次数、看不到他人明细）。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private IDashboardService dashboardService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 工作台汇总
     *
     * @param request 请求
     * @return 汇总数据
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        return Result.success(dashboardService.summary(userName));
    }

    /**
     * 近 7 日登录趋势
     *
     * @return 按天升序
     */
    @GetMapping("/loginTrend")
    public Result<List<LoginTrendVO>> loginTrend() {
        return Result.success(dashboardService.loginTrend());
    }
}
