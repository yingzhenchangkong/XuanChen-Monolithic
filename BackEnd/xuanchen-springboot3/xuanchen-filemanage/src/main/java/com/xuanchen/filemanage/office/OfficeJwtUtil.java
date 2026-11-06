package com.xuanchen.filemanage.office;

import com.xuanchen.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具-->ONLYOFFICE JWT（HS256，零第三方依赖）
 * <p>用途：① 给编辑器配置整体签名（DS 校验防篡改）；② 给 /office/download 下载地址签
 * 短时效令牌（DS 匿名拉取文档）；③ 校验 DS 回调请求的 token。密钥与 DS 的 JWT_SECRET 相同。</p>
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Component
public class OfficeJwtUtil {

    private static final String HEADER_B64 = base64Url(Map.of("alg", "HS256", "typ", "JWT"));

    @Autowired
    private OfficeProperties properties;

    /** 用配置密钥对 claims 签发 JWT */
    public String sign(Map<String, Object> claims) {
        return sign(claims, properties.getJwtSecret());
    }

    /** 校验并解析 JWT（签名不符抛 IllegalArgumentException，过期抛 IllegalStateException） */
    public Map<String, Object> verify(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("缺少 JWT");
        }
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT 格式错误");
        }
        String expectedSig = hmac(parts[0] + "." + parts[1], properties.getJwtSecret());
        if (!constantTimeEquals(expectedSig, parts[2])) {
            throw new IllegalArgumentException("JWT 签名校验失败");
        }
        Map<String, Object> claims = JsonUtils.fromJson(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8), Map.class);
        Object exp = claims.get("exp");
        if (exp instanceof Number n && n.longValue() < System.currentTimeMillis() / 1000) {
            throw new IllegalStateException("JWT 已过期");
        }
        return claims;
    }

    /** 生成文档下载短时效令牌（DS 匿名拉取用） */
    public String downloadToken(String fileId, int ttlSeconds) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("fid", fileId);
        claims.put("exp", System.currentTimeMillis() / 1000 + ttlSeconds);
        return sign(claims);
    }

    private static String sign(Map<String, Object> claims, String secret) {
        String payload = base64Url(claims);
        String signingInput = HEADER_B64 + "." + payload;
        return signingInput + "." + hmac(signingInput, secret);
    }

    private static String base64Url(Object obj) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JsonUtils.toJson(obj).getBytes(StandardCharsets.UTF_8));
    }

    private static String hmac(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
