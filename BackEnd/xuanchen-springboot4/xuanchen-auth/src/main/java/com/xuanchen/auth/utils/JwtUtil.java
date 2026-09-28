package com.xuanchen.auth.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;

/**
 * JWT 工具类
 *
 * @author XuanChen
 * @date 2025-03-12
 */
public class JwtUtil {
    /**
     * token 过期时间（秒）：1 天。JWT 签发与 Redis 会话 TTL 共用此唯一定义，
     * 禁止再各自乘倍数，避免出现"JWT 已过期但 Redis 会话仍在"的有效期矛盾
     */
    public static final long EXPIRE_SECONDS = 60L * 60 * 24;
    /**
     * token 过期时间 1天（毫秒，仅用于 JWT Date 计算）
     */
    public static final long EXPIRE_TIME = EXPIRE_SECONDS * 1000; //24小时
    /**
     * 验证码 token 有效期（秒）
     */
    public static final long CAPTCHA_EXPIRE_SECONDS = 300;

    /**
     * 验证码签名密钥：不再硬编码在源码中。
     * 由 AuthSecretInitializer 在应用启动时从配置 xuanchen.captcha.secret 注入；
     * 未配置时每次启动随机生成（仅适用于单实例部署，多实例必须显式配置同一密钥）。
     */
    private static volatile String captchaSecret;

    /**
     * 登录态 JWT 服务端签名密钥：禁止使用用户密码哈希（改密即全员掉线、且密码哈希本不应作为密钥材料）。
     * 由 AuthSecretInitializer 从配置 xuanchen.jwt.secret（环境变量 XUANCHEN_JWT_SECRET）注入；
     * 未配置时每次启动随机生成（重启后所有登录态失效，多实例必须显式配置同一密钥）。
     */
    private static volatile String jwtSecret;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 初始化验证码密钥（启动时调用一次）
     *
     * @param secret 配置中的密钥，空则随机生成
     */
    public static void initCaptchaSecret(String secret) {
        if (secret != null && !secret.isBlank()) {
            captchaSecret = secret;
        } else if (captchaSecret == null) {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            captchaSecret = HexFormat.of().formatHex(bytes);
        }
    }

    /**
     * 初始化登录态 JWT 密钥（启动时调用一次）
     *
     * @param secret 配置中的密钥，空则随机生成
     */
    public static void initJwtSecret(String secret) {
        if (secret != null && !secret.isBlank()) {
            jwtSecret = secret;
        } else if (jwtSecret == null) {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            jwtSecret = HexFormat.of().formatHex(bytes);
        }
    }

    private static String getCaptchaSecret() {
        if (captchaSecret == null) {
            synchronized (JwtUtil.class) {
                if (captchaSecret == null) {
                    initCaptchaSecret(null);
                }
            }
        }
        return captchaSecret;
    }

    private static String getJwtSecret() {
        if (jwtSecret == null) {
            synchronized (JwtUtil.class) {
                if (jwtSecret == null) {
                    initJwtSecret(null);
                }
            }
        }
        return jwtSecret;
    }

    /**
     * 生成登录态 token（使用服务端配置密钥），EXPIRE_TIME 后过期。
     * <p>
     * 失败时抛出 {@link IllegalStateException} 而不是返回 null：token 签发属于服务端关键操作，
     * 调用方一旦拿到 null 就会出现"登录成功但 token 为 null"（null 被写入 Redis 键/返回给前端）；
     * 抛出后由全局异常处理器返回 500，语义正确。注意登录入口必须保证先签发 token、再写 Redis 会话，
     * 这样抛异常时不会残留脏会话。
     *
     * @param username 用户名（不允许为空）
     * @return 签名后的 token，永不为 null
     */
    public static String sign(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空，无法签发登录 token");
        }
        try {
            Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);
            Algorithm algorithm = Algorithm.HMAC256(getJwtSecret());
            return JWT.create().withClaim("username", username).withExpiresAt(date).sign(algorithm);
        } catch (IllegalArgumentException e) {
            // 密钥等入参本身非法，直接向上抛，不被下面的兜底包装吞掉真实原因
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("登录态 token 签发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建验证码 token。与 {@link #sign} 同理：失败抛 {@link IllegalStateException}，绝不返回 null，
     * 由调用方/全局异常处理器统一处理
     */
    public static String createCaptchaToken(String captchaId, long expireSeconds) {
        if (captchaId == null || captchaId.isBlank()) {
            throw new IllegalArgumentException("captchaId 不能为空，无法签发验证码 token");
        }
        try {
            Date date = new Date(System.currentTimeMillis() + expireSeconds * 1000);
            Algorithm algorithm = Algorithm.HMAC256(getCaptchaSecret() + captchaId);
            return JWT.create().withClaim("captchaId", captchaId).withExpiresAt(date).sign(algorithm);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("验证码 token 签发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 校验登录态 token 的签名、过期时间与用户名是否匹配（使用服务端配置密钥）
     *
     * @param token    登录态 token
     * @param username 用户名
     * @return true 合法
     */
    public static boolean verify(String token, String username) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(getJwtSecret());
            JWTVerifier verifier = JWT.require(algorithm).withClaim("username", username).build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证验证码token的签名和内容
     */
    public static boolean verifyCaptchaToken(String token, String captchaId) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(getCaptchaSecret() + captchaId);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("captchaId", captchaId).build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 token 中的信息，无需 secret 解密也能获取
     *
     * @param token
     * @return
     */
    public static String getUsername(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("username").asString();
        } catch (Exception e) {
            return null;
        }
    }
}
