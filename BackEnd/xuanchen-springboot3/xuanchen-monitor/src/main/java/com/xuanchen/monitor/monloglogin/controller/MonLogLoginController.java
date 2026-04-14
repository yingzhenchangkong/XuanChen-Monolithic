package com.xuanchen.monitor.monloglogin.controller;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.aspect.annotation.DictMethod;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.monitor.monloglogin.entity.MonLogLogin;
import com.xuanchen.monitor.monloglogin.service.IMonLogLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 控制器-->日志 登录
 *
 * @author XuanChen
 * @date 2026-02-04
 */
@RestController
@RequestMapping("/monitor/log/login")
public class MonLogLoginController {
    @Autowired
    private IMonLogLoginService monLogLoginService;

    /**
     * 分页列表查询
     *
     * @param monLogLogin
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @DictMethod
    @GetMapping("/list")
    public Result list(MonLogLogin monLogLogin,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<MonLogLogin> queryWrapper = new QueryWrapper<>();
        // 用户名精确查询
        if (StringUtil.isNotEmpty(monLogLogin.getUserName())) {
            queryWrapper.eq("user_name", monLogLogin.getUserName());
        }
        // 登录状态精确查询
        if (StringUtil.isNotEmpty(monLogLogin.getStatus())) {
            queryWrapper.eq("status", monLogLogin.getStatus());
        }
        // 时间区间查询
        if (monLogLogin.getBeginTime() != null) {
            queryWrapper.ge("login_time", monLogLogin.getBeginTime());
        }
        if (monLogLogin.getEndTime() != null) {
            queryWrapper.le("login_time", monLogLogin.getEndTime());
        }
        queryWrapper.orderByDesc("login_time");
        Page<MonLogLogin> page = new Page<>(pageNo, pageSize);
        IPage<MonLogLogin> pageList = monLogLoginService.page(page, queryWrapper);
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
        List<MonLogLogin> list = monLogLoginService.list();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String title = "登录日志";
        String fileName = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        FastExcel.write(response.getOutputStream())
                .head(MonLogLogin.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet(title)
                .doWrite(list);
    }
}
