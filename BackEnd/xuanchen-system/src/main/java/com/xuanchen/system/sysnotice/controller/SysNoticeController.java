package com.xuanchen.system.sysnotice.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.server.WebSocketServer;
import com.xuanchen.system.sysnotice.entity.SysNotice;
import com.xuanchen.system.sysnotice.entity.SysNoticeStatus;
import com.xuanchen.system.sysnotice.service.ISysNoticeService;
import com.xuanchen.system.sysnotice.service.ISysNoticeStatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
    private WebSocketServer webSocketServer;

    @GetMapping("/listUser")
    public Result listUser(SysNotice sysNotice,
                           @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                           HttpServletRequest req) {
        String userName = JwtUtil.getUsername(req.getHeader("XC-ACCESS-TOKEN"));
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
    public Result listManage(SysNotice sysNotice,
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
    public Result listManageStatus(SysNoticeStatus sysNoticeStatus,
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
    public Result getNoticeCount(HttpServletRequest request) {
        String userName = JwtUtil.getUsername(request.getHeader("XC-ACCESS-TOKEN"));
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
     * 发布通知
     *
     * @param sysNotice
     * @param request
     * @return
     */
    @PostMapping(value = "/issue")
    public Result issue(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        sysNoticeService.save(sysNotice);
        List<String> listUser = sysNotice.getListUser();
        List<SysNoticeStatus> listStatus = new ArrayList<>();
        for (String userName : listUser) {
            SysNoticeStatus sysNoticeStatus = new SysNoticeStatus();
            sysNoticeStatus.setNoticeId(sysNotice.getId());
            sysNoticeStatus.setUserId(userName);
            listStatus.add(sysNoticeStatus);
        }
        sysNoticeStatusService.saveBatch(listStatus);
        List<Map<String, Object>> listNotice = new ArrayList<>();
        for (String userName : listUser) {
            Map<String, Object> map = getNoticeCountByUserName(userName);
            listNotice.add(map);
        }
        webSocketServer.sendMessage(JSONObject.toJSONString(listNotice));
        return Result.success("发布成功!");
    }

    /**
     * 撤销
     *
     * @param sysNotice
     * @return
     */
    @PostMapping(value = "/cancel")
    public Result cancel(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        String userName = JwtUtil.getUsername(request.getHeader("XC-ACCESS-TOKEN"));
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
    public Result recover(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        String userName = JwtUtil.getUsername(request.getHeader("XC-ACCESS-TOKEN"));
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
        List<Map<String, Object>> listNotice = new ArrayList<>();
        for (SysNoticeStatus item : listSysNoticeStatus) {
            Map<String, Object> map = getNoticeCountByUserName(item.getUserId());
            listNotice.add(map);
        }
        webSocketServer.sendMessage(JSONObject.toJSONString(listNotice));
    }


    /**
     * 标记已读
     *
     * @param sysNotice
     * @return
     */
    @PostMapping(value = "/setRead")
    public Result setRead(@RequestBody SysNotice sysNotice, HttpServletRequest request) {
        SysNoticeStatus sysNoticeStatus = new SysNoticeStatus();
        sysNoticeStatus.setId(sysNotice.getNoticeStatusId());
        sysNoticeStatus.setReadStatus(true);
        sysNoticeStatus.setReadTime(LocalDateTime.now());
        sysNoticeStatusService.updateById(sysNoticeStatus);

        String userName = JwtUtil.getUsername(request.getHeader("XC-ACCESS-TOKEN"));
        Map<String, Object> map = getNoticeCountByUserName(userName);
        List<Map<String, Object>> listNotice = new ArrayList<>();
        listNotice.add(map);
        webSocketServer.sendMessage(JSONObject.toJSONString(listNotice));
        return Result.success("设置成功！");
    }

    /**
     * 批量标记已读
     *
     * @param jsonObject
     * @return
     */
    @PostMapping(value = "/setReadBatch")
    public Result setReadBatch(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        List<String> listIds = jsonObject.getList("ids", String.class);
        UpdateWrapper<SysNoticeStatus> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("read_status", true)
                .set("read_time", LocalDateTime.now())
                .in("id", listIds);
        sysNoticeStatusService.update(updateWrapper);

        String userName = JwtUtil.getUsername(request.getHeader("XC-ACCESS-TOKEN"));
        Map<String, Object> map = getNoticeCountByUserName(userName);
        List<Map<String, Object>> listNotice = new ArrayList<>();
        listNotice.add(map);
        webSocketServer.sendMessage(JSONObject.toJSONString(listNotice));
        return Result.success("设置成功！");
    }


    /**
     * 标记全部已读
     *
     * @return
     */
    @PostMapping(value = "/setReadAll")
    public Result setReadAll(HttpServletRequest request) {
        String userName = JwtUtil.getUsername(request.getHeader("XC-ACCESS-TOKEN"));
        UpdateWrapper<SysNoticeStatus> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userName).eq("read_status", false);
        updateWrapper.set("read_status", true);
        updateWrapper.set("read_time", LocalDateTime.now());
        sysNoticeStatusService.update(updateWrapper);

        Map<String, Object> map = getNoticeCountByUserName(userName);
        List<Map<String, Object>> listNotice = new ArrayList<>();
        listNotice.add(map);
        webSocketServer.sendMessage(JSONObject.toJSONString(listNotice));
        return Result.success("设置成功！");
    }
}
