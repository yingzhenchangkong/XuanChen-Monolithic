package com.xuanchen.auth.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

/**
 * JWT 工具类
 *
 * @author XuanChen
 * @date 2025-03-12
 */
public class JwtUtil {
    /**
     * token 过期时间 1天
     */
    public static final long EXPIRE_TIME = 1000 * 60 * 60 * 24; //24小时
    private static final String CAPTCHA_SECRET = "xuanchen_captcha_secret_key"; // 验证码专用密钥

    /**
     * 生成签名, EXPIRE_TIME 后过期
     *
     * @param username
     * @param secret
     * @return
     */
    public static String sign(String username, String secret) {
        try {
            Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create().withClaim("username", username).withExpiresAt(date).sign(algorithm);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建验证码 token
     */
    public static String createCaptchaToken(String captchaId, long expireSeconds) {
        try {
            Date date = new Date(System.currentTimeMillis() + expireSeconds * 1000);
            Algorithm algorithm = Algorithm.HMAC256(CAPTCHA_SECRET + captchaId);
            return JWT.create().withClaim("captchaId", captchaId).withExpiresAt(date).sign(algorithm);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 校验 token 是否正确
     *
     * @param token
     * @param username
     * @param secret
     * @return
     */
    public static boolean verify(String token, String username, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("username", username).build();
            DecodedJWT jwt = verifier.verify(token);
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
            Algorithm algorithm = Algorithm.HMAC256(CAPTCHA_SECRET + captchaId);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("captchaId", captchaId).build();
            DecodedJWT jwt = verifier.verify(token);
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
