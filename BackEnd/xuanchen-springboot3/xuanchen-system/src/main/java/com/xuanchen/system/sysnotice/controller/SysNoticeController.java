package com.xuanchen.system.sysnotice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.server.WebSocketServer;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysnotice.dto.NoticeReadBatchDTO;
import com.xuanchen.system.sysnotice.dto.NoticeReadDTO;
import com.xuanchen.system.sysnotice.entity.SysNotice;
import com.xuanchen.system.sysnotice.entity.SysNoticeStatus;
import com.xuanchen.system.sysnotice.service.ISysNoticeService;
import com.xuanchen.system.sysnotice.service.ISysNoticeStatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SysNoticeController {
    @Autowired
    private ISysNoticeService sysNoticeService;
    @Autowired
    private ISysNoticeStatusService sysNoticeStatusService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

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
     * @param req
     * @return
     */
    @GetMapping(value = "/listManage")
    @RequiresRoles("admin")
    public Result<IPage<SysNotice>> listManage(SysNotice sysNotice,
                             @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                             @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                             HttpServletRequest req) {
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
     * @param req
     * @return
     */
    @GetMapping("/listManageStatus")
    @RequiresRoles("admin")
    public Result<IPage<SysNoticeStatus>> listManageStatus(SysNoticeStatus sysNoticeStatus,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
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
     * 仅向指定用户的在线连接推送其未读数（单元素数组，与前端 forEach 解析约定一致）。
     * 未认证连接在握手阶段即被拒绝、从不入连接池，定向推送也保证其他用户
     * （含在线管理员）收不到不属于自己的通知接收人与未读数。
     */
    private void pushNoticeCount(String userName) {
        WebSocketServer.sendMessage(userName,
                JsonUtils.toJson(List.of(getNoticeCountByUserName(userName))));
    }

    /**
     * 发布通知
     * 事务约束：sys_notice 主表与 sys_notice_status 批量行必须同一事务提交，
     * 批量插入失败时主表通知一并回滚，不留"发了但无人收到"的孤儿通知；
     * WebSocket 未读推送放在事务提交后（afterCommit）执行，避免回滚后给用户推送了不存在的通知。
     *
     * @param sysNotice
     * @param request
     * @return
     */
    @PostMapping(value = "/issue")
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> issue(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
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
        // 提交成功后再推送在线用户未读数（逐接收人定向推送，不向全部连接广播，避免接收人列表/他人未读数泄露）
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
    @RequiresRoles("admin")
    public Result<String> cancel(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        if (!cancelRecover(sysNotice.getId(), 2, userName)) {
            return Result.badRequest("通知不存在或已被删除！");
        }
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
    @RequiresRoles("admin")
    public Result<String> recover(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        if (!cancelRecover(sysNotice.getId(), 1, userName)) {
            return Result.badRequest("通知不存在或已被删除！");
        }
        return Result.success("恢复成功！");
    }

    /**
     * 撤销/恢复
     *
     * @param id
     * @param status
     * @param userName
     */
    private boolean cancelRecover(String id, Integer status, String userName) {
        SysNotice sysNotice = new SysNotice();
        sysNotice.setId(id);
        sysNotice.setStatus(status);
        // 影响行数为 0（通知 id 不存在或已被删除）时返回 false，由调用方拒绝并提示
        boolean updated = sysNoticeService.updateById(sysNotice);
        if (!updated) {
            return false;
        }

        QueryWrapper<SysNoticeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("notice_id", sysNotice.getId());
        List<SysNoticeStatus> listSysNoticeStatus = sysNoticeStatusService.list(queryWrapper);
        //逐接收人定向推送各自的未读数
        for (SysNoticeStatus item : listSysNoticeStatus) {
            pushNoticeCount(item.getUserId());
        }
        return true;
    }


    /**
     * 标记已读
     * 防越权（IDOR）：更新条件强制带 user_id=当前登录用户，通知状态 id 即使被枚举或篡改，
     * 也只能标记属于本人的通知；影响行数为 0 时直接拒绝。
     *
     * @param dto 仅含通知状态行 id，独立 DTO 接参，不复用 SysNotice 实体
     * @return
     */
    @PostMapping(value = "/setRead")
    public Result<String> setRead(@RequestBody NoticeReadDTO dto, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        String noticeStatusId = dto == null ? null : dto.getNoticeStatusId();
        if (StringUtil.isEmpty(noticeStatusId)) {
            return Result.badRequest("通知状态ID不能为空！");
        }
        UpdateWrapper<SysNoticeStatus> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("read_status", 1)
                .set("read_time", LocalDateTime.now())
                .eq("id", noticeStatusId)
                .eq("user_id", userName);
        boolean updated = sysNoticeStatusService.update(updateWrapper);
        if (!updated) {
            return Result.badRequest("通知不存在或无权操作他人通知！");
        }

        pushNoticeCount(userName);
        return Result.success("设置成功！");
    }

    /**
     * 批量标记已读
     * 防越权（IDOR）：id 列表与当前登录用户做归属约束，只能批量标记本人的通知。
     * ids 为空/null 时幂等返回（不拼 in()，避免非法 SQL）；空白元素先过滤。
     *
     * @param dto 待标记的状态行 id 集合
     * @return
     */
    @PostMapping(value = "/setReadBatch")
    public Result<String> setReadBatch(@RequestBody NoticeReadBatchDTO dto, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        List<String> listIds = dto == null ? null : dto.getIds();
        if (listIds == null || listIds.isEmpty()) {
            return Result.success("设置成功！");
        }
        //去除空白元素，避免无意义条件；过滤后为空同样幂等返回
        List<String> validIds = listIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .toList();
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
