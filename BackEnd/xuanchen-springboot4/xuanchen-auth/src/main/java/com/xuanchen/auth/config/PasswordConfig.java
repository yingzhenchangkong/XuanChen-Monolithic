package com.xuanchen.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 口令编码器基础配置。
 * <p>
 * 独立于 {@link SecurityConfig}：PasswordEncoder 是被业务服务层（PasswordCodec 等）
 * 依赖的通用基础 Bean，若定义在持有 JwtAuthenticationFilter 的 SecurityConfig 中，
 * 会形成「过滤器→UserDetailsService→认证服务→PasswordCodec→PasswordEncoder→
 * SecurityConfig→过滤器」的 Bean 构造闭环，Spring Boot 3+ 默认禁止循环引用导致启动失败。
 * 迁出后依赖方向保持单向（安全配置 → 过滤器 → 服务 → PasswordCodec → 编码器）。
 *
 * @author XuanChen
 * @date 2026-10-10
 */
@Configuration
public class PasswordConfig {

    /**
     * 密码编码器：BCrypt 强哈希算法（cost=10，输出 $2a$ 前缀、60 字符，自带随机盐）。
     * 所有密码必须通过此编码器加密。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
