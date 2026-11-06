package com.xuanchen.generator.loggenerator.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xuanchen.common.entity.Result;
import com.xuanchen.generator.loggenerator.entity.LogGenerator;
import com.xuanchen.generator.loggenerator.service.ILogGeneratorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制器 --> 代码生成记录
 *
 * @author XuanChen
 * @date 2026-10-19
 */
@RestController
@PreAuthorize("hasRole('admin')")
@RequestMapping("/tool/generator/log")
public class LogGeneratorController {

    @Autowired
    private ILogGeneratorService genGenerateLogService;

    /**
     * 按表配置分页查询生成记录
     *
     * @param tableId  表配置 id
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<IPage<LogGenerator>> list(@RequestParam(name = "tableId", required = false) String tableId,
                                              @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(genGenerateLogService.pageByTable(tableId, pageNo, pageSize));
    }

    /**
     * 清空指定表配置的生成记录
     *
     * @param tableId 表配置 id
     * @return 结果
     */
    @DeleteMapping("/clear")
    public Result<String> clear(@RequestParam(name = "tableId", required = false) String tableId) {
        genGenerateLogService.clearByTable(tableId);
        return Result.success("生成记录已清空");
    }
}
