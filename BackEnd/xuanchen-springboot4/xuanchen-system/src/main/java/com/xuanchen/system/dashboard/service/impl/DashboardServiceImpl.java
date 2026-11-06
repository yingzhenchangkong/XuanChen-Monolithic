package com.xuanchen.system.dashboard.service.impl;

import com.xuanchen.system.dashboard.mapper.DashboardMapper;
import com.xuanchen.system.dashboard.service.IDashboardService;
import com.xuanchen.system.dashboard.vo.DashboardNoticeVO;
import com.xuanchen.system.dashboard.vo.DashboardTodoVO;
import com.xuanchen.system.dashboard.vo.LoginTrendVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service-->工作台聚合数据
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Service
public class DashboardServiceImpl implements IDashboardService {

    private static final int TOP_SIZE = 5;
    private static final int TREND_DAYS = 7;
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> summary(String userName) {
        LocalDateTime todayBegin = LocalDate.now().atStartOfDay();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("unreadNoticeCount", dashboardMapper.countUnreadNotice(userName));
        map.put("pendingTodoCount", dashboardMapper.countPendingTodo(userName));
        map.put("todayLoginCount", dashboardMapper.countTodayLogin(todayBegin));

        List<DashboardNoticeVO> notices = dashboardMapper.listUnreadNotice(userName, TOP_SIZE);
        map.put("notices", notices == null ? new ArrayList<>() : notices);

        List<DashboardTodoVO> todos = dashboardMapper.listPendingTodo(userName, TOP_SIZE);
        map.put("todos", todos == null ? new ArrayList<>() : todos);
        return map;
    }

    @Override
    public List<LoginTrendVO> loginTrend() {
        LocalDate today = LocalDate.now();
        LocalDateTime beginTime = today.minusDays(TREND_DAYS - 1L).atStartOfDay();
        LocalDateTime endTime = today.plusDays(1L).atStartOfDay();
        List<LoginTrendVO> listDb = dashboardMapper.countLoginGroupByDay(beginTime, endTime);

        // 按天补零：数据库无登录记录的日期也要有数据点，保证前端折线连续
        Map<String, Long> countByDay = new LinkedHashMap<>();
        if (listDb != null) {
            for (LoginTrendVO item : listDb) {
                countByDay.put(item.getDate(), item.getCount() == null ? 0L : item.getCount());
            }
        }
        List<LoginTrendVO> result = new ArrayList<>(TREND_DAYS);
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate day = today.minusDays(TREND_DAYS - 1L - i);
            LoginTrendVO vo = new LoginTrendVO();
            vo.setDate(day.format(DAY_FORMATTER));
            vo.setCount(countByDay.getOrDefault(vo.getDate(), 0L));
            result.add(vo);
        }
        return result;
    }
}
