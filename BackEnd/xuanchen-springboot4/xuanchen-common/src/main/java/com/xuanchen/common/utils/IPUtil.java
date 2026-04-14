package com.xuanchen.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 工具类-->IP
 *
 * @author XuanChen
 * @date 2026-02-07
 */
public final class IPUtil {
    /**
     * 获取客户端真实IP地址
     *
     * @param request HTTP 请求对象
     * @return 客户端IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
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
