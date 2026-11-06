package com.xuanchen.system.systodo.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.systodo.dto.TodoIdDTO;
import com.xuanchen.system.systodo.entity.SysTodo;
import com.xuanchen.system.systodo.service.ISysTodoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 控制器-->系统待办
 *
 * <p>防越权（IDOR）约定：所有写操作（编辑/完成/删除）的条件都强制带
 * user_name = 当前登录用户，只能操作本人的待办；影响行数为 0 时拒绝。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@RestController
@RequestMapping("/system/todo")
public class SysTodoController {

    @Autowired
    private ISysTodoService sysTodoService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 我的待办分页列表
     *
     * @param sysTodo  查询条件（title 模糊 / status 精确）
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param req      请求
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<IPage<SysTodo>> list(SysTodo sysTodo,
                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                       HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        sysTodo.setUserName(userName);
        Page<SysTodo> page = new Page<>(pageNo, pageSize);
        return Result.success(sysTodoService.listMine(page, sysTodo));
    }

    /**
     * 新增待办（归属当前登录用户）
     *
     * @param sysTodo 待办内容
     * @param req     请求
     * @return 结果
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody SysTodo sysTodo, HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        if (StringUtil.isEmpty(sysTodo.getTitle())) {
            return Result.badRequest("待办标题不能为空！");
        }
        if (sysTodo.getTitle().length() > 200) {
            return Result.badRequest("待办标题不能超过200字！");
        }
        if (sysTodo.getContent() != null && sysTodo.getContent().length() > 1000) {
            return Result.badRequest("待办内容不能超过1000字！");
        }
        // 归属与初始状态由服务端决定，不接受客户端传入
        sysTodo.setId(null);
        sysTodo.setUserName(userName);
        sysTodo.setStatus(0);
        sysTodo.setFinishTime(null);
        sysTodoService.save(sysTodo);
        return Result.success("添加成功！");
    }

    /**
     * 编辑待办（仅本人，仅未完成状态可编辑）
     *
     * @param sysTodo 待办内容（须含 id）
     * @param req     请求
     * @return 结果
     */
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody SysTodo sysTodo, HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        if (StringUtil.isEmpty(sysTodo.getId())) {
            return Result.badRequest("待办ID不能为空！");
        }
        if (StringUtil.isEmpty(sysTodo.getTitle())) {
            return Result.badRequest("待办标题不能为空！");
        }
        if (sysTodo.getContent() != null && sysTodo.getContent().length() > 1000) {
            return Result.badRequest("待办内容不能超过1000字！");
        }
        // 归属字段不允许通过编辑修改
        sysTodo.setUserName(null);
        sysTodo.setStatus(null);
        sysTodo.setFinishTime(null);
        UpdateWrapper<SysTodo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", sysTodo.getId())
                .eq("user_name", userName)
                .eq("status", 0);
        boolean updated = sysTodoService.update(sysTodo, updateWrapper);
        if (!updated) {
            return Result.badRequest("待办不存在、已完成或无权操作他人待办！");
        }
        return Result.success("修改成功！");
    }

    /**
     * 完成待办（仅本人）
     *
     * @param dto 仅含待办 id
     * @param req 请求
     * @return 结果
     */
    @PostMapping("/finish")
    public Result<String> finish(@RequestBody TodoIdDTO dto, HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        String id = dto == null ? null : dto.getId();
        if (StringUtil.isEmpty(id)) {
            return Result.badRequest("待办ID不能为空！");
        }
        UpdateWrapper<SysTodo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", 1)
                .set("finish_time", LocalDateTime.now())
                .eq("id", id)
                .eq("user_name", userName);
        boolean updated = sysTodoService.update(updateWrapper);
        if (!updated) {
            return Result.badRequest("待办不存在或无权操作他人待办！");
        }
        return Result.success("完成成功！");
    }

    /**
     * 删除待办（仅本人，物理删除）
     *
     * @param dto 仅含待办 id
     * @param req 请求
     * @return 结果
     */
    @PostMapping("/delete")
    public Result<String> delete(@RequestBody TodoIdDTO dto, HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        String id = dto == null ? null : dto.getId();
        if (StringUtil.isEmpty(id)) {
            return Result.badRequest("待办ID不能为空！");
        }
        boolean removed = sysTodoService.remove(
                new UpdateWrapper<SysTodo>().eq("id", id).eq("user_name", userName));
        if (!removed) {
            return Result.badRequest("待办不存在或无权操作他人待办！");
        }
        return Result.success("删除成功！");
    }

    /**
     * 删除待办（仅本人，物理删除；供列表页 deleteAction 使用）
     *
     * @param id  待办 id
     * @param req 请求
     * @return 结果
     */
    @DeleteMapping("/delete")
    public Result<String> deleteById(@RequestParam(name = "id", required = true) String id, HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        boolean removed = sysTodoService.remove(
                new UpdateWrapper<SysTodo>().eq("id", id).eq("user_name", userName));
        if (!removed) {
            return Result.badRequest("待办不存在或无权操作他人待办！");
        }
        return Result.success("删除成功!");
    }

    /**
     * 批量删除待办（仅本人的待办会被删除，他人 id 静默忽略）
     *
     * @param ids 逗号分隔的待办 id 集合
     * @param req 请求
     * @return 结果
     */
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids, HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        List<String> listIds = Arrays.stream(ids.split(","))
                .filter(item -> !StringUtil.isEmpty(item))
                .toList();
        if (listIds.isEmpty()) {
            return Result.success("删除成功!");
        }
        sysTodoService.remove(
                new UpdateWrapper<SysTodo>().in("id", listIds).eq("user_name", userName));
        return Result.success("删除成功!");
    }
}
