package com.xuanchen.businessprocess.instance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.businessprocess.instance.dto.BpmSubmitDTO;
import com.xuanchen.businessprocess.instance.entity.BpmInstance;
import com.xuanchen.businessprocess.instance.service.IBpmInstanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 控制器-->审批流程实例（登录即可）
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@RestController
@RequestMapping("/businessprocess/instance")
public class BpmInstanceController {

    @Autowired
    private IBpmInstanceService instanceService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 发起申请
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody BpmSubmitDTO dto, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = instanceService.submit(userName, dto);
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("提交成功!");
    }

    /**
     * 我的申请分页
     */
    @GetMapping("/mine")
    public Result<IPage<BpmInstance>> mine(BpmInstance query,
                                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                          HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(instanceService.listMine(new Page<>(pageNo, pageSize),
                userName, query.getStatus(), query.getBusinessSummary()));
    }

    /**
     * 审批历史时间线
     */
    @GetMapping("/records")
    public Result<Map<String, Object>> records(@RequestParam(name = "instanceId") String instanceId) {
        return Result.success(instanceService.records(instanceId));
    }

    /**
     * 流程图渲染数据（XML + 当前/已走节点，前端高亮）
     */
    @GetMapping("/diagramState")
    public Result<Map<String, Object>> diagramState(@RequestParam(name = "instanceId") String instanceId) {
        return Result.success(instanceService.diagramState(instanceId));
    }

    /**
     * 发起人撤回
     */
    @PostMapping("/revoke")
    public Result<String> revoke(@RequestBody BpmInstance body, HttpServletRequest request) {
        if (body.getId() == null || body.getId().isBlank()) {
            return Result.badRequest("缺少流程实例ID！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = instanceService.revoke(userName, body.getId());
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("撤回成功!");
    }

    /**
     * 发起人催办（WebSocket 提醒当前审批人）
     */
    @PostMapping("/urge")
    public Result<String> urge(@RequestBody BpmInstance body, HttpServletRequest request) {
        if (body.getId() == null || body.getId().isBlank()) {
            return Result.badRequest("缺少流程实例ID！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = instanceService.urge(userName, body.getId());
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("已催办!");
    }
}
