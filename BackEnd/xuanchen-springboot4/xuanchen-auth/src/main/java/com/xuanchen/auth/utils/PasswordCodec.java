package com.xuanchen.auth.utils;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 口令编解码器：BCrypt 慢哈希（SB4 版，不依赖 Shiro）。
 * <p>
 * 直接复用容器中 Spring Security 的 {@link PasswordEncoder}
 * （由 PasswordConfig 定义，cost=10，输出 $2a$ 前缀、60 字符，自带随机盐），
 * 与 SB3 侧 BCryptPasswordEncoder/Shiro-bcrypt 的存量哈希格式互通，无需迁移。
 *
 * @author XuanChen
 * @date 2026-10-02
 */
@Component
public class PasswordCodec {

    private final PasswordEncoder passwordEncoder;

    public PasswordCodec(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 使用 BCrypt 对明文口令做单向哈希（每次调用内置随机盐，同一明文输出不同）
     *
     * @param rawPassword 明文口令
     * @return $2a$ 开头的 BCrypt 哈希
     */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验明文口令与库内 BCrypt 哈希是否匹配（常量时间比较，防计时侧信道）。
     *
     * @param rawPassword 明文口令
     * @param storedHash  库内 BCrypt 哈希
     * @return true 匹配
     */
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (IllegalArgumentException e) {
            //库内哈希不是合法的 BCrypt crypt 串：按不匹配处理，避免脏数据导致登录报 500
            return false;
        }
    }
}
