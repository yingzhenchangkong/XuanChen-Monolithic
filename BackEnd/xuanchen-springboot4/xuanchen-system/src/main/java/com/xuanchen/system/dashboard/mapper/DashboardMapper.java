package com.xuanchen.system.dashboard.mapper;

import com.xuanchen.system.dashboard.vo.DashboardNoticeVO;
import com.xuanchen.system.dashboard.vo.DashboardTodoVO;
import com.xuanchen.system.dashboard.vo.LoginTrendVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper-->工作台聚合查询
 *
 * <p>聚合涉及多模块表（sys_notice / sys_todo / log_login），单库直查，
 * 不引入模块间 Java 依赖；各 SQL 语义与来源模块保持一致：</p>
 * <ul>
 *     <li>未读公告：同 SysNoticeStatusMapper.getNoticeCountByUserName（status=1 且 read_status=0）</li>
 *     <li>登录统计：同 log_login 的成功登录（status=1）口径</li>
 * </ul>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface DashboardMapper {

    /**
     * 未读公告数
     *
     * @param userName 登录名
     * @return 未读数量
     */
    Integer countUnreadNotice(@Param("userName") String userName);

    /**
     * 未完成待办数
     *
     * @param userName 登录名
     * @return 数量
     */
    Integer countPendingTodo(@Param("userName") String userName);

    /**
     * 今日成功登录次数（全系统）
     *
     * @param beginTime 今日零点
     * @return 次数
     */
    Integer countTodayLogin(@Param("beginTime") LocalDateTime beginTime);

    /**
     * 未读公告 top N
     *
     * @param userName 登录名
     * @param limit    条数
     * @return 通知列表
     */
    List<DashboardNoticeVO> listUnreadNotice(@Param("userName") String userName, @Param("limit") int limit);

    /**
     * 未完成待办 top N（按优先级升序、截止时间升序）
     *
     * @param userName 登录名
     * @param limit    条数
     * @return 待办列表
     */
    List<DashboardTodoVO> listPendingTodo(@Param("userName") String userName, @Param("limit") int limit);

    /**
     * 区间内按天成功登录次数（缺日由 service 层补零）
     *
     * @param beginTime 起始时间（含）
     * @param endTime   结束时间（含）
     * @return 按天统计
     */
    List<LoginTrendVO> countLoginGroupByDay(@Param("beginTime") LocalDateTime beginTime,
                                            @Param("endTime") LocalDateTime endTime);
}
