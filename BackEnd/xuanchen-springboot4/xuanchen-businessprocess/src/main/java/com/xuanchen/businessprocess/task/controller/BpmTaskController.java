package com.xuanchen.businessprocess.task.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.businessprocess.task.entity.BpmTask;
import com.xuanchen.businessprocess.task.service.IBpmTaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 控制器-->审批任务（登录即可，按 assignee 隔离）
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@RestController
@RequestMapping("/businessprocess/task")
public class BpmTaskController {

    @Autowired
    private IBpmTaskService taskService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 待我审批分页
     */
    @GetMapping("/todo")
    public Result<IPage<BpmTask>> todo(BpmTask query,
                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                      HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(taskService.listTodo(new Page<>(pageNo, pageSize), userName,
                query.getBusinessSummary()));
    }

    /**
     * 我已审批分页
     */
    @GetMapping("/done")
    public Result<IPage<BpmTask>> done(BpmTask query,
                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                      HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(taskService.listDone(new Page<>(pageNo, pageSize), userName,
                query.getBusinessSummary()));
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    public Result<String> approve(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String taskId = body.get("taskId");
        String opinion = body.get("opinion");
        if (taskId == null || taskId.isBlank()) {
            return Result.badRequest("缺少审批任务ID！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = taskService.approve(userName, taskId, opinion);
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("审批通过!");
    }

    /**
     * 审批驳回
     */
    @PostMapping("/reject")
    public Result<String> reject(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String taskId = body.get("taskId");
        if (taskId == null || taskId.isBlank()) {
            return Result.badRequest("缺少审批任务ID！");
        }
        String opinion = body.get("opinion");
        if (opinion == null || opinion.isBlank()) {
            return Result.badRequest("驳回必须填写审批意见！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = taskService.reject(userName, taskId, opinion);
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("已驳回!");
    }

    /**
     * 驳回至指定节点（流程不办结，打回重审）
     */
    @PostMapping("/rejectToNode")
    public Result<String> rejectToNode(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String taskId = body.get("taskId");
        String targetNodeKey = body.get("targetNodeKey");
        if (taskId == null || taskId.isBlank()) {
            return Result.badRequest("缺少审批任务ID！");
        }
        String opinion = body.get("opinion");
        if (opinion == null || opinion.isBlank()) {
            return Result.badRequest("驳回必须填写审批意见！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = taskService.rejectToNode(userName, taskId, targetNodeKey, opinion);
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("已驳回至指定节点!");
    }

    /**
     * 可驳回目标节点（本实例已走过的审批节点）
     */
    @GetMapping("/rejectTargets")
    public Result<List<Map<String, String>>> rejectTargets(@RequestParam(name = "taskId") String taskId) {
        return Result.success(taskService.rejectTargets(taskId));
    }

    /**
     * 转办
     */
    @PostMapping("/transfer")
    public Result<String> transfer(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String taskId = body.get("taskId");
        if (taskId == null || taskId.isBlank()) {
            return Result.badRequest("缺少审批任务ID！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = taskService.transfer(userName, taskId, body.get("targetUser"), body.get("opinion"));
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("转办成功!");
    }
}
