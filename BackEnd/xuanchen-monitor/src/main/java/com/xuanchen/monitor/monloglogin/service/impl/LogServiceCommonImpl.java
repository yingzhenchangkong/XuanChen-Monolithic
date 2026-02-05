package com.xuanchen.monitor.monloglogin.service.impl;

import com.xuanchen.common.service.ILogServiceCommon;
import com.xuanchen.monitor.monloglogin.entity.MonLogLogin;
import com.xuanchen.monitor.monloglogin.service.IMonLogLoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service接口实现类-->日志 通用
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Service
public class LogServiceCommonImpl implements ILogServiceCommon {
    @Autowired
    private IMonLogLoginService monLogLoginService;

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
        log.setIpAddress(getClientIpAddress(request)); // 客户端IP
        log.setStatus(status); // 登录状态
        log.setDescription(description); // 描述信息

        monLogLoginService.save(log); // 保存到数据库
    }

    /**
     * 获取客户端真实IP地址
     *
     * @param request HTTP 请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0]; // 多个IP时取第一个
    }
}
