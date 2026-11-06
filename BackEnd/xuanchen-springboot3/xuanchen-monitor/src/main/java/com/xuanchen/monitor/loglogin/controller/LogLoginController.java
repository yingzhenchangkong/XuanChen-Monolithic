package com.xuanchen.monitor.loglogin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.aspect.annotation.DictMethod;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.excel.FesodSheetExportUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.monitor.loglogin.entity.LogLogin;
import com.xuanchen.monitor.loglogin.service.ILogLoginService;
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
 * 控制器-->日志 登录
 *
 * @author XuanChen
 * @date 2026-02-04
 */
@RestController
@RequiresRoles("admin")
@RequestMapping("/monitor/log/login")
public class LogLoginController {
    @Autowired
    private ILogLoginService logLoginService;

    /**
     * 分页列表查询
     *
     * @param logLogin
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @DictMethod
    @GetMapping("/list")
    public Result<IPage<LogLogin>> list(LogLogin logLogin,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<LogLogin> queryWrapper = new QueryWrapper<>();
        // 用户名精确查询
        if (StringUtil.isNotEmpty(logLogin.getUserName())) {
            queryWrapper.eq("user_name", logLogin.getUserName());
        }
        // 登录状态精确查询
        if (StringUtil.isNotEmpty(logLogin.getStatus())) {
            queryWrapper.eq("status", logLogin.getStatus());
        }
        // 时间区间查询
        if (logLogin.getBeginTime() != null) {
            queryWrapper.ge("login_time", logLogin.getBeginTime());
        }
        if (logLogin.getEndTime() != null) {
            queryWrapper.le("login_time", logLogin.getEndTime());
        }
        queryWrapper.orderByDesc("login_time");
        Page<LogLogin> page = new Page<>(pageNo, pageSize);
        IPage<LogLogin> pageList = logLoginService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 导出excel
     *
     * @param response
     * @throws IOException
     */
    @GetMapping(value = "/exportExcel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        // 主键游标分批查询 + 分批写入，避免 list() 全量加载导致 OOM
        FesodSheetExportUtil.export(response, "登录日志", LogLogin.class, LogLogin::getId, (lastId, batchSize) -> {
            QueryWrapper<LogLogin> queryWrapper = new QueryWrapper<>();
            if (StringUtil.isNotEmpty(lastId)) {
                queryWrapper.gt("id", lastId);
            }
            queryWrapper.orderByAsc("id");
            return logLoginService.page(FesodSheetExportUtil.cursorPage(batchSize), queryWrapper).getRecords();
        });
    }
}

