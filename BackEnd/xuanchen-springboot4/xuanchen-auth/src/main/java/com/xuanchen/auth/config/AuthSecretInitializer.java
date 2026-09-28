package com.xuanchen.auth.config;

import com.xuanchen.auth.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 认证签名密钥初始化器
 * <p>
 * 启动时注入两类密钥：
 * 1、{@code xuanchen.captcha.secret}（环境变量 XUANCHEN_CAPTCHA_SECRET）：滑块验证码 token；
 * 2、{@code xuanchen.jwt.secret}（环境变量 XUANCHEN_JWT_SECRET）：登录态 JWT。
 * 未配置时由 JwtUtil 随机生成，避免源码内置固定密钥；多实例/生产必须显式配置同一密钥
 * （prod profile 下为必填项）。
 *
 * @author XuanChen
 * @date 2026-09-17
 */
@Slf4j
@Component
public class AuthSecretInitializer {

    @Value("${xuanchen.captcha.secret:}")
    private String captchaSecret;

    @Value("${xuanchen.jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        JwtUtil.initCaptchaSecret(captchaSecret);
        JwtUtil.initJwtSecret(jwtSecret);
        // 注册给 common 模块 WebSocket 握手使用，保证 WS 与 HTTP 执行同一套 JWT 签名校验
        com.xuanchen.common.server.WsAuthBridge.registerTokenVerifier(JwtUtil::verify);
        if (captchaSecret == null || captchaSecret.isBlank()) {
            log.warn("未配置 xuanchen.captcha.secret（XUANCHEN_CAPTCHA_SECRET），验证码密钥已随机生成，多实例部署将互不通用");
        }
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.warn("未配置 xuanchen.jwt.secret（XUANCHEN_JWT_SECRET），登录态密钥已随机生成，重启后所有登录态失效，多实例部署将互不通用");
        }
    }
}
