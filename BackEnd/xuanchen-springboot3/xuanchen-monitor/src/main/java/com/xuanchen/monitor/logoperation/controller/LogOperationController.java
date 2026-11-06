package com.xuanchen.monitor.logoperation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.aspect.annotation.DictMethod;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.excel.FesodSheetExportUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.monitor.logoperation.entity.LogOperation;
import com.xuanchen.monitor.logoperation.service.ILogOperationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 控制器-->操作日志
 *
 * @author XuanChen
 * @date 2026-02-07
 */
@RestController
@RequiresRoles("admin")
@RequestMapping("/monitor/log/operation")
public class LogOperationController {
    @Autowired
    private ILogOperationService logOperationService;

    /**
     * 分页列表查询
     *
     * @param logOperation 查询条件
     * @param pageNo          当前页码
     * @param pageSize        每页条数
     * @param req             HTTP请求对象
     * @return 分页结果
     */
    @DictMethod
    @GetMapping("/list")
    public Result<IPage<LogOperation>> list(LogOperation logOperation,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<LogOperation> queryWrapper = new QueryWrapper<>();
        // 用户名精确查询
        if (StringUtil.isNotEmpty(logOperation.getUserName())) {
            queryWrapper.eq("user_name", logOperation.getUserName());
        }
        // 登录状态精确查询
        if (StringUtil.isNotEmpty(logOperation.getStatus())) {
            queryWrapper.eq("status", logOperation.getStatus());
        }
        // 时间区间查询
        if (logOperation.getBeginTime() != null) {
            queryWrapper.ge("operation_time", logOperation.getBeginTime());
        }
        if (logOperation.getEndTime() != null) {
            queryWrapper.le("operation_time", logOperation.getEndTime());
        }
        queryWrapper.orderByDesc("operation_time");
        Page<LogOperation> page = new Page<>(pageNo, pageSize);
        IPage<LogOperation> pageList = logOperationService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 导出Excel
     *
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @GetMapping(value = "/exportExcel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        // 主键游标分批查询 + 分批写入，避免 list() 全量加载导致 OOM
        FesodSheetExportUtil.export(response, "操作日志", LogOperation.class, LogOperation::getId, (lastId, batchSize) -> {
            QueryWrapper<LogOperation> queryWrapper = new QueryWrapper<>();
            if (StringUtil.isNotEmpty(lastId)) {
                queryWrapper.gt("id", lastId);
            }
            queryWrapper.orderByAsc("id");
            return logOperationService.page(FesodSheetExportUtil.cursorPage(batchSize), queryWrapper).getRecords();
        });
    }
}

