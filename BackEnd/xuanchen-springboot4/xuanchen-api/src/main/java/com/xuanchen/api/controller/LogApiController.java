package com.xuanchen.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xuanchen.api.entity.LogApi;
import com.xuanchen.api.service.ILogApiService;
import com.xuanchen.common.entity.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制器-->数据接口服务-调用日志（系统监控 → 接口日志）
 * <p>
 * 只读查询：日志由接口调用链路异步落库，监控侧不提供增删改。
 * 查询条件：接口编码（模糊）、触发方式、是否成功、调用时间区间。
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@RestController
@PreAuthorize("hasRole('admin')")
@RequestMapping("/monitor/log/api")
public class LogApiController {

    @Autowired
    private ILogApiService logApiService;

    /**
     * 分页列表查询
     *
     * @param logApi   查询条件（apiCode/triggerType/success/beginTime/endTime）
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<IPage<LogApi>> list(LogApi logApi,
                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(logApiService.pageQuery(logApi, pageNo, pageSize));
    }
}
