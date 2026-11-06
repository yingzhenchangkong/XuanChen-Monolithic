package com.xuanchen.filemanage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 配置类-->启用定时任务（文件中心回收站清理/共享过期撤销等）
 * 放在 filemanage 模块内而非启动类，保持模块自包含。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
