package com.xuanchen.common.utils;

import java.util.Random;

/**
 * 工具类-->盐值
 *
 * @author XuanChen
 * @date 2025-03-24
 */
public final class SaltUtil {
    public static String getSalt(int n) {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%&*".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            char str = chars[new Random().nextInt(chars.length)];
            sb.append(str);
        }
        return sb.toString();
    }
}