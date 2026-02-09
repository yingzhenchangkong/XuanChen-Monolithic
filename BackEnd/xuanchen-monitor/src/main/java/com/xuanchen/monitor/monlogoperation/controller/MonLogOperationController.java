package com.xuanchen.monitor.monlogoperation.controller;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.aspect.annotation.DictMethod;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.monitor.monlogoperation.entity.MonLogOperation;
import com.xuanchen.monitor.monlogoperation.service.IMonLogOperationService;
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
 * 控制器-->操作日志
 *
 * @author XuanChen
 * @date 2026-02-07
 */
@RestController
@RequestMapping("/monitor/log/operation")
public class MonLogOperationController {
    @Autowired
    private IMonLogOperationService monLogOperationService;

    /**
     * 分页列表查询
     *
     * @param monLogOperation 查询条件
     * @param pageNo          当前页码
     * @param pageSize        每页条数
     * @param req             HTTP请求对象
     * @return 分页结果
     */
    @DictMethod
    @GetMapping("/list")
    public Result list(MonLogOperation monLogOperation,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<MonLogOperation> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(monLogOperation.getUserName())) {
            queryWrapper.like("user_name", monLogOperation.getUserName());
        }
        queryWrapper.orderByDesc("operation_time");
        Page<MonLogOperation> page = new Page<>(pageNo, pageSize);
        IPage<MonLogOperation> pageList = monLogOperationService.page(page, queryWrapper);
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
        List<MonLogOperation> list = monLogOperationService.list();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String title = "操作日志";
        String fileName = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        FastExcel.write(response.getOutputStream())
                .head(MonLogOperation.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet(title)
                .doWrite(list);
    }
}
