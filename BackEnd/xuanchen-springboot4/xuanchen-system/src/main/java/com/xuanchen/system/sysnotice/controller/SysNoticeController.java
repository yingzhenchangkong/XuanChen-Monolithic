package com.xuanchen.system.sysnotice.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.server.WebSocketServer;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysnotice.dto.NoticeReadBatchDTO;
import com.xuanchen.system.sysnotice.dto.NoticeReadDTO;
import com.xuanchen.system.sysnotice.entity.SysNotice;
import com.xuanchen.system.sysnotice.entity.SysNoticeStatus;
import com.xuanchen.system.sysnotice.service.ISysNoticeService;
import com.xuanchen.system.sysnotice.service.ISysNoticeStatusService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->通知
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@RestController
@RequestMapping("/system/notice")
@RequiredArgsConstructor
public class SysNoticeController {
    private final ISysNoticeService sysNoticeService;
    private final ISysNoticeStatusService sysNoticeStatusService;
    private final IAuthServiceCommon authServiceCommon;

    @GetMapping("/listUser")
    public Result<IPage<SysNotice>> listUser(SysNotice sysNotice,
                                             @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                             @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                             HttpServletRequest req) {
        String userName = authServiceCommon.getUserNameByToken(req.getHeader("XC-ACCESS-TOKEN"));
        sysNotice.setUserName(userName);
        Page<SysNotice> page = new Page<>(pageNo, pageSize);
        IPage<SysNotice> pageList = sysNoticeService.listUser(page, sysNotice);
        return Result.success(pageList);
    }

    /**
     * 获取通知列表（管理）
     *
     * @param sysNotice
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping(value = "/listManage")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<SysNotice>> listManage(SysNotice sysNotice,
                                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<SysNotice> page = new Page<>(pageNo, pageSize);
        IPage<SysNotice> pageList = sysNoticeService.listManage(page, sysNotice);
        return Result.success(pageList);
    }

    /**
     * 获取通知列表（管理）明细
     *
     * @param sysNoticeStatus
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/listManageStatus")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<SysNoticeStatus>> listManageStatus(SysNoticeStatus sysNoticeStatus,
                                                           @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<SysNoticeStatus> page = new Page<>(pageNo, pageSize);
        IPage<SysNoticeStatus> pageList = sysNoticeStatusService.list(page, sysNoticeStatus);
        return Result.success(pageList);
    }

    /**
     * 获取当前用户未读通知数量
     *
     * @param request
     * @return
     */
    @GetMapping(value = "/getNoticeCount")
    public Result<Map<String, Object>> getNoticeCount(HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        Map<String, Object> map = getNoticeCountByUserName(userName);
        return Result.success(map);
    }

    private Map<String, Object> getNoticeCountByUserName(String userName) {
        Integer count = sysNoticeStatusService.getNoticeCountByUserName(userName);
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userName);
        map.put("count", count);
        return map;
    }

    /**
     * 仅向指定用户在线连接推送其未读数（单元素数组，与前端 forEach 解析约定保持一致）
     */
    private void pushNoticeCount(String userName) {
        WebSocketServer.sendMessage(userName,
                JSONObject.toJSONString(List.of(getNoticeCountByUserName(userName))));
    }

    /**
     * 发布通知
     * 事务约束：sys_notice 主表与 sys_notice_status 批量行必须同一事务提交，
     * 批量插入失败时主表通知一并回滚，不留"发了但无人收到"的孤儿通知；
     * WebSocket 未读推送放在事务提交后（afterCommit）执行，避免回滚后给用户推送了不存在的通知。
     *
     * @param sysNotice
     * @return
     */
    @PostMapping(value = "/issue")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> issue(@RequestBody SysNotice sysNotice) {
        List<String> listUser = sysNotice.getListUser();
        if (listUser == null || listUser.isEmpty()) {
            return Result.badRequest("接收通知的用户不能为空！");
        }
        sysNoticeService.save(sysNotice);
        List<SysNoticeStatus> listStatus = new ArrayList<>();
        for (String userName : listUser) {
            // StringUtil.isEmpty 只识别 null/""/"null"，纯空白字符串需额外 trim 判定，
            // 否则 ["   "] 会被当成合法接收人落库
            if (StringUtil.isEmpty(userName) || userName.trim().isEmpty()) {
                continue;
            }
            SysNoticeStatus sysNoticeStatus = new SysNoticeStatus();
            sysNoticeStatus.setNoticeId(sysNotice.getId());
            sysNoticeStatus.setUserId(userName.trim());
            listStatus.add(sysNoticeStatus);
        }
        if (listStatus.isEmpty()) {
            // 主表已插入：必须抛异常回滚事务，不能正常 return（否则留下无接收人的通知）
            throw new BusinessException(400, "接收通知的用户不能为空！");
        }
        sysNoticeStatusService.saveBatch(listStatus);
        // 提交成功后再推送在线用户未读数
        List<String> targetUsers = listStatus.stream().map(SysNoticeStatus::getUserId).distinct().toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String userName : targetUsers) {
                    pushNoticeCount(userName);
                }
            }
        });
        return Result.success("发布成功!");
    }

    /**
     * 撤销
     *
     * @param sysNotice
     * @return
     */
    @PostMapping(value = "/cancel")
    @PreAuthorize("hasRole('admin')")
    public Result<String> cancel(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        cancelRecover(sysNotice.getId(), 2, userName);
        return Result.success("撤销成功！");
    }

    /**
     * 恢复
     *
     * @param sysNotice
     * @param request
     * @return
     */
    @PostMapping(value = "/recover")
    @PreAuthorize("hasRole('admin')")
    public Result<String> recover(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        cancelRecover(sysNotice.getId(), 1, userName);
        return Result.success("恢复成功！");
    }

    /**
     * 撤销/恢复
     *
     * @param id
     * @param status
     * @param userName
     */
    private void cancelRecover(String id, Integer status, String userName) {
        SysNotice sysNotice = new SysNotice();
        sysNotice.setId(id);
        sysNotice.setStatus(status);
        sysNoticeService.updateById(sysNotice);

        QueryWrapper<SysNoticeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("notice_id", sysNotice.getId());
        List<SysNoticeStatus> listSysNoticeStatus = sysNoticeStatusService.list(queryWrapper);
        for (SysNoticeStatus item : listSysNoticeStatus) {
            pushNoticeCount(item.getUserId());
        }
    }


    /**
     * 标记已读。
     * <p>
     * 归属校验：更新条件同时限定 id 与当前登录用户（user_id），
     * 防止凭他人的 noticeStatusId 把别人的通知标记已读（IDOR）。
     * 通知不存在或不属于当前用户统一返回相同错误，避免通过响应差异枚举他人通知 id。
     *
     * @param dto 仅含 noticeStatusId
     * @return 结果
     */
    @PostMapping(value = "/setRead")
    public Result<String> setRead(@RequestBody NoticeReadDTO dto, HttpServletRequest request) {
        String userName = currentUserName(request);
        String noticeStatusId = dto == null ? null : dto.getNoticeStatusId();
        if (noticeStatusId == null || noticeStatusId.isBlank()) {
            throw new BusinessException(400, "通知状态id不能为空");
        }
        UpdateWrapper<SysNoticeStatus> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("read_status", 1)
                .set("read_time", LocalDateTime.now())
                .eq("id", noticeStatusId)
                .eq("user_id", userName);
        boolean updated = sysNoticeStatusService.update(updateWrapper);
        if (!updated) {
            throw new BusinessException(400, "通知不存在或无权操作");
        }

        pushNoticeCount(userName);
        return Result.success("设置成功！");
    }

    /**
     * 批量标记已读。
     * <p>
     * ids 为空/null 时幂等返回（不拼 {@code in()}，避免非法 SQL 导致 500）；
     * 更新条件同样强制限定当前登录用户，只可能更新属于自己的通知状态行。
     *
     * @param dto 待标记的状态行 id 集合
     * @return 结果
     */
    @PostMapping(value = "/setReadBatch")
    public Result<String> setReadBatch(@RequestBody NoticeReadBatchDTO dto, HttpServletRequest request) {
        String userName = currentUserName(request);
        List<String> listIds = dto == null ? null : dto.getIds();
        if (listIds == null || listIds.isEmpty()) {
            return Result.success("设置成功！");
        }
        // 去除空白元素，避免无意义条件；过滤后为空同样幂等返回
        List<String> validIds = listIds.stream().filter(id -> id != null && !id.isBlank()).toList();
        if (validIds.isEmpty()) {
            return Result.success("设置成功！");
        }
        UpdateWrapper<SysNoticeStatus> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("read_status", 1)
                .set("read_time", LocalDateTime.now())
                .in("id", validIds)
                .eq("user_id", userName);
        sysNoticeStatusService.update(updateWrapper);

        pushNoticeCount(userName);
        return Result.success("设置成功！");
    }

    /**
     * 从请求头令牌解析当前登录用户名（即 sys_notice_status.user_id 的归属值）
     */
    private String currentUserName(HttpServletRequest request) {
        return authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
    }


    /**
     * 标记全部已读
     *
     * @return
     */
    @PostMapping(value = "/setReadAll")
    public Result<String> setReadAll(HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        UpdateWrapper<SysNoticeStatus> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userName).eq("read_status", 0);
        updateWrapper.set("read_status", 1);
        updateWrapper.set("read_time", LocalDateTime.now());
        sysNoticeStatusService.update(updateWrapper);

        pushNoticeCount(userName);
        return Result.success("设置成功！");
    }
}
