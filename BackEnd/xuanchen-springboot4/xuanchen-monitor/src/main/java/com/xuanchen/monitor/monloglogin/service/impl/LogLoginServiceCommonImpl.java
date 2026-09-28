package com.xuanchen.monitor.monloglogin.service.impl;

import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.service.ILogLoginServiceCommon;
import com.xuanchen.common.utils.IPUtil;
import com.xuanchen.monitor.monloglogin.entity.MonLogLogin;
import com.xuanchen.monitor.monloglogin.service.IMonLogLoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service接口实现类-->日志 登录
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Service
@RequiredArgsConstructor
public class LogLoginServiceCommonImpl implements ILogLoginServiceCommon {
    private final IMonLogLoginService monLogLoginService;

    /**
     * 记录登录日志
     *
     * @param username    用户名
     * @param status      登录状态
     * @param description 描述信息
     * @param request     HTTP 请求对象
     */
    public void recordLoginLog(String username, Boolean status, String description, HttpServletRequest request) {
        MonLogLogin log = new MonLogLogin();
        log.setUserName(username); // 用户名
        log.setLoginTime(LocalDateTime.now()); // 登录时间
        log.setIpAddress(IPUtil.getClientIpAddress(request)); // 客户端IP
        log.setStatus(Boolean.TRUE.equals(status) ? CommonConst.STATUS_ENABLED : CommonConst.STATUS_DISABLED); // 登录状态（1成功，0失败）
        log.setDescription(description); // 描述信息

        monLogLoginService.save(log); // 保存到数据库
    }
}
