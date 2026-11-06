package com.xuanchen.filemanage.task;

import com.xuanchen.filemanage.service.IFileChunkService;
import com.xuanchen.filemanage.service.IFileInfoService;
import com.xuanchen.filemanage.service.IFileShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务-->文件中心二期清理（回收站超期彻底删除 + 过期共享撤销）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Component
public class FileCenterCleanTask {

    @Autowired
    private IFileInfoService fileInfoService;

    @Autowired
    private IFileShareService fileShareService;

    @Autowired
    private IFileChunkService fileChunkService;

    /**
     * 回收站保留天数（超过即彻底删除）
     */
    @Value("${xuanchen.fileinfo.recycle-keep-days:30}")
    private int recycleKeepDays;

    /**
     * 每日凌晨 3 点执行
     */
    @Scheduled(cron = "${xuanchen.fileinfo.clean-cron:0 0 3 * * ?}")
    public void clean() {
        try {
            int revoked = fileShareService.revokeExpired();
            if (revoked > 0) {
                log.info("过期共享撤销完成：{} 条", revoked);
            }
        } catch (Exception e) {
            log.error("过期共享撤销失败", e);
        }
        try {
            int cleaned = fileInfoService.cleanExpiredRecycle(recycleKeepDays);
            if (cleaned > 0) {
                log.info("回收站超期清理完成：{} 条（保留 {} 天）", cleaned, recycleKeepDays);
            }
        } catch (Exception e) {
            log.error("回收站超期清理失败", e);
        }
        try {
            fileChunkService.clearExpired();
        } catch (Exception e) {
            log.error("分片上传超期任务清理失败", e);
        }
    }
}
