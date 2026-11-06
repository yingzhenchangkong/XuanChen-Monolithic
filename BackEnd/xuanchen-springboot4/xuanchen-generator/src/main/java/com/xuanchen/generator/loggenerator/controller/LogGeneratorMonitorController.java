package com.xuanchen.generator.loggenerator.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xuanchen.common.entity.Result;
import com.xuanchen.generator.loggenerator.entity.LogGenerator;
import com.xuanchen.generator.loggenerator.service.ILogGeneratorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制器 --> 代码生成日志（系统监控 → 代码生成日志）
 * <p>
 * 与 {@link LogGeneratorController}（代码生成器页面里"按表配置"查看记录）的区别：
 * 这里不限定表配置，面向全局排查，可按表名/表注释/模块/生成方式/操作人/成功标识/时间区间筛选。
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@RestController
@PreAuthorize("hasRole('admin')")
@RequestMapping("/monitor/log/generator")
public class LogGeneratorMonitorController {

    @Autowired
    private ILogGeneratorService logGeneratorService;

    /**
     * 分页列表查询
     *
     * @param logGenerator 查询条件（tableName/tableComment/moduleName/generateType/
     *                     createBy/success/beginTime/endTime）
     * @param pageNo       页码
     * @param pageSize     每页条数
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<IPage<LogGenerator>> list(LogGenerator logGenerator,
                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(logGeneratorService.pageForMonitor(logGenerator, pageNo, pageSize));
    }
}
