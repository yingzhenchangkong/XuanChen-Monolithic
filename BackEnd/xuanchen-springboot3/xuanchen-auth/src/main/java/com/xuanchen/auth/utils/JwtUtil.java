package com.xuanchen.auth.utils;

import com.xuanchen.common.utils.JsonUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.xuanchen.common.entity.Result;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;

/**
 * JWT 工具类
 *
 * @author XuanChen
 * @date 2025-03-12
 */
@Slf4j
public class JwtUtil {

    /**
     * token 过期时间 1天
     */
    public static final long EXPIRE_TIME = 1000 * 60 * 60 * 24; //24小时

    /**
     * 验证码 token 有效期（秒）
     */
    public static final long CAPTCHA_EXPIRE_SECONDS = 300;

    /**
     * 验证码签名密钥：不硬编码在源码中。
     * 由 AuthSecretInitializer 在应用启动时从配置 xuanchen.captcha.secret 注入；
     * 未配置时首次使用随机生成（仅适用于单实例部署，多实例必须显式配置同一密钥）。
     */
    private static volatile String captchaSecret;

    /**
     * 登录态 JWT 服务端签名密钥：禁止使用用户密码哈希（改密即全员掉线、且密码哈希本不应作为密钥材料）。
     * 由 AuthSecretInitializer 从配置 xuanchen.jwt.secret（环境变量 XUANCHEN_JWT_SECRET）注入；
     * 未配置时首次使用随机生成（重启后所有登录态失效，多实例必须显式配置同一密钥）。
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
     * 验签失败的细分类型，供调用方区分过期与无效凭证
     */
    public enum VerifyFailure {
        /**
         * token 已过 exp 有效期
         */
        TOKEN_EXPIRED,
        /**
         * token 无效（签名错/算法不符/claim 不匹配/格式非法）
         */
        TOKEN_INVALID
    }

    /**
     * 生成登录态 token（使用服务端配置密钥 xuanchen.jwt.secret），EXPIRE_TIME 后过期。
     * <p>
     * 签发失败属服务端配置异常，必须抛出而不是返回 null——
     * 否则 null token 会被当作登录成功结果下发给客户端，并在 Redis 写入 "null" 幽灵会话。
     *
     * @param username 用户名
     * @return 签名后的 token，永不为 null
     */
    public static String sign(String username) {
        try {
            Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);
            Algorithm algorithm = Algorithm.HMAC256(getJwtSecret());
            return JWT.create().withClaim("username", username).withExpiresAt(date).sign(algorithm);
        } catch (IllegalArgumentException e) {
            //密钥等入参非法属编程/配置错误，直接向上抛
            throw e;
        } catch (Exception e) {
            log.error("JWT签发失败, username:{}", username, e);
            throw new IllegalStateException("Token签发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 校验 token 是否正确（使用服务端配置密钥）
     *
     * @param token    待校验 token
     * @param username token 中应携带的用户名
     * @return true 合法
     */
    public static boolean verify(String token, String username) {
        return verifyFailureReason(token, username) == null;
    }

    /**
     * 校验 token 并返回细分失败原因，便于区分"过期"与"签名错误/被篡改"。
     *
     * @param token    待校验 token
     * @param username token 中应携带的用户名
     * @return null 表示校验通过；否则为失败原因
     */
    public static VerifyFailure verifyFailureReason(String token, String username) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(getJwtSecret());
            JWTVerifier verifier = JWT.require(algorithm).withClaim("username", username).build();
            verifier.verify(token);
            return null;
        } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
            return VerifyFailure.TOKEN_EXPIRED;
        } catch (Exception e) {
            // SignatureVerificationException / AlgorithmMismatchException /
            // InvalidClaimException / JWTDecodeException 等统一归为凭证无效
            return VerifyFailure.TOKEN_INVALID;
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

    /**
     * 创建验证码 token。签名密钥拼接 captchaId，使 token 与本次滑块绑定。
     * 失败时抛出 {@link IllegalStateException}，绝不返回 null，
     * 避免出现"验证成功但 token 为 null"的矛盾响应，由全局异常/调用方统一处理
     *
     * @param captchaId     验证码唯一标识
     * @param expireSeconds 有效期（秒）
     * @return 签名后的验证码 token，永不为 null
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
            // 密钥等入参本身非法，直接向上抛，不被下面的兜底包装吞掉真实原因
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("验证码 token 签发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证验证码 token 的签名、过期时间与 captchaId 是否匹配
     *
     * @param token     验证码 token
     * @param captchaId 验证码唯一标识
     * @return true 合法
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

    public static void responseError(ServletResponse response, Integer code, String errorMsg) {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader("Content-type", "text/html;charset=UTF-8");
        Result<String> jsonResult = new Result<>(code, errorMsg);
        OutputStream os = null;
        try {
            os = httpServletResponse.getOutputStream();
            httpServletResponse.setCharacterEncoding("utf-8");
            httpServletResponse.setStatus(code);
            os.write(JsonUtils.toJson(jsonResult).getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
        } catch (IOException ex) {
            // 客户端断开连接等场景下错误响应无法写出，属常见情况，warn 记录即可
            log.warn("JWT校验失败响应写出异常: {}", ex.getMessage());
        }
    }
}
