package com.xuanchen.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 工具类-->终端类型识别
 * 根据 User-Agent 识别请求来源：手机端/电脑端
 *
 * @author XuanChen
 * @date 2026-06-09
 */
public final class DeviceTypeUtil {

    /** 终端类型：电脑端 */
    public static final String DEVICE_PC = "PC";

    /** 终端类型：手机端 */
    public static final String DEVICE_MOBILE = "移动端";

    /** 终端类型：未知 */
    public static final String DEVICE_UNKNOWN = "未知";

    /**
     * 常见移动端关键字
     */
    private static final String[] MOBILE_KEYWORDS = {
            "android", "iphone", "ipad", "ipod", "windows phone",
            "mobile", "blackberry", "symbian", "webos", "opera mini",
            "ucweb", "micromessenger", "harmony", "huawei", "xiaomi", "miui"
    };

    /**
     * 根据请求识别终端类型
     *
     * @param request HTTP 请求对象
     * @return 终端类型：PC / 移动端 / 未知
     */
    public static String detectDeviceType(HttpServletRequest request) {
        if (request == null) {
            return DEVICE_UNKNOWN;
        }
        String userAgent = request.getHeader("User-Agent");
        return detectDeviceType(userAgent);
    }

    /**
     * 根据 User-Agent 字符串识别终端类型
     *
     * @param userAgent User-Agent 字符串
     * @return 终端类型：PC / 移动端 / 未知
     */
    public static String detectDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return DEVICE_UNKNOWN;
        }
        String ua = userAgent.toLowerCase();
        for (String keyword : MOBILE_KEYWORDS) {
            if (ua.contains(keyword)) {
                return DEVICE_MOBILE;
            }
        }
        return DEVICE_PC;
    }

    private DeviceTypeUtil() {}
}
