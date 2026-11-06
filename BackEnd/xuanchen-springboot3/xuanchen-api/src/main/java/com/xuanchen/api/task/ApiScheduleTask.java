package com.xuanchen.api.task;

import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.entity.ApiSchedule;
import com.xuanchen.api.service.IApiInvokeService;
import com.xuanchen.api.mapper.ApiScheduleMapper;
import com.xuanchen.api.service.impl.ApiServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时采集调度器
 * 每分钟扫描 api_schedule 中到期的任务（enabled=1 且 next_run_time<=now），
 * 直接调用执行引擎（不走 HTTP），结果落 api_data（触发方式=定时采集）。
 * 注意：多实例部署时需保证仅一个实例开启本调度（xuanchen.api.scheduler-enabled）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "xuanchen.api.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ApiScheduleTask {

    @Autowired
    private ApiServiceImpl apiService;
    @Autowired
    private IApiInvokeService invokeService;
    @Autowired
    private ApiScheduleMapper scheduleMapper;

    /**
     * 固定延迟 60 秒扫描一次（启动后延迟 30 秒首跑，避开启动高峰）
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void scan() {
        LocalDateTime now = LocalDateTime.now();
        List<ApiSchedule> dueList;
        try {
            dueList = apiService.listDueSchedules(now);
        } catch (Exception e) {
            log.error("定时采集：扫描到期任务失败", e);
            return;
        }
        for (ApiSchedule sched : dueList) {
            runOne(sched, now);
        }
    }

    private void runOne(ApiSchedule sched, LocalDateTime now) {
        String apiCode = sched.getApiCode();
        try {
            Api api = invokeService.getPublishedByCode(apiCode);
            if (api == null) {
                log.warn("定时采集跳过：接口不存在或未发布 code={}", apiCode);
            } else {
                invokeService.execute(api, java.util.Map.of(), IApiInvokeService.TRIGGER_SCHEDULE, true);
                log.info("定时采集完成 code={}", apiCode);
            }
        } catch (Exception e) {
            log.error("定时采集执行失败 code={}", apiCode, e);
        } finally {
            //无论成败推进时间窗，避免失败任务死循环重跑
            try {
                ApiSchedule update = new ApiSchedule();
                update.setId(sched.getId());
                update.setLastRunTime(now);
                update.setNextRunTime(nextTime(sched.getCron(), now));
                scheduleMapper.updateById(update);
            } catch (Exception e) {
                log.error("定时采集：更新执行时间失败 id={}", sched.getId(), e);
            }
        }
    }

    private LocalDateTime nextTime(String cron, LocalDateTime from) {
        if (StringUtil.isEmpty(cron)) {
            return from.plusHours(1);
        }
        try {
            LocalDateTime next = CronExpression.parse(cron).next(from);
            return next == null ? from.plusYears(100) : next;
        } catch (Exception e) {
            return from.plusHours(1);
        }
    }
}
