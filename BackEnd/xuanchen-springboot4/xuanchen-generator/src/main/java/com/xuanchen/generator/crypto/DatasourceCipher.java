package com.xuanchen.generator.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 数据源连接密码的 AES/GCM 加解密工具。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>AES-256-GCM（带认证标签，防密文被篡改），12 字节随机 IV，每次加密结果不同；</li>
 *   <li>密钥由配置 xuanchen.crypto.datasource-key（环境变量 XUANCHEN_DATASOURCE_SECRET）注入，
 *       经 SHA-256 派生为 32 字节 AES 密钥，不允许硬编码；</li>
 *   <li>密文统一以 ENC(...) 包裹，便于与历史明文区分；遇到不带前缀的旧数据按明文原样返回，
 *       保证存量数据平滑过渡，下一次保存时自动重新加密；</li>
 *   <li>密钥不可随机生成（不同于 JWT）：密钥丢失将导致全部已存密码不可解密，
 *       因此缺失密钥时由 {@link DatasourceCryptoInitializer} 在启动阶段 fail-fast。</li>
 * </ul>
 *
 * @author XuanChen
 * @date 2026-09-24
 */
public final class DatasourceCipher {

    /** 密文前缀，与历史明文做区分 */
    public static final String CIPHER_PREFIX = "ENC(";
    private static final String CIPHER_SUFFIX = ")";
    /** 接口响应中用于占位的掩码，回传该值表示"不修改密码" */
    public static final String MASK = "******";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static volatile SecretKeySpec keySpec;

    private DatasourceCipher() {
    }

    /**
     * 启动阶段注入密钥（SHA-256 派生 AES-256）。
     *
     * @param secret 配置的密钥原文，不能为空
     */
    public static void init(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "数据源密码加密密钥 xuanchen.crypto.datasource-key（XUANCHEN_DATASOURCE_SECRET）未配置，"
                            + "该密钥用于持久化数据加解密，缺失时禁止启动");
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            keySpec = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化数据源加密密钥失败", e);
        }
    }

    /**
     * 加密明文密码。null 原样返回；已经是 ENC(...) 密文的不再重复加密，防止双重加密。
     *
     * @param plain 明文密码
     * @return ENC(...) 包裹的密文
     */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || isCipherText(plain)) {
            return plain;
        }
        SecretKeySpec key = requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined) + CIPHER_SUFFIX;
        } catch (Exception e) {
            throw new IllegalStateException("数据源密码加密失败", e);
        }
    }

    /**
     * 解密密文。null 原样返回；不带 ENC(...) 前缀的视为历史明文直接返回（平滑兼容）；
     * 带前缀但解密失败说明密钥不匹配，抛出异常避免用错误密码静默连接。
     *
     * @param stored 库内存储值
     * @return 明文密码
     */
    public static String decrypt(String stored) {
        if (stored == null || stored.isEmpty() || !isCipherText(stored)) {
            return stored;
        }
        SecretKeySpec key = requireKey();
        try {
            String base64 = stored.substring(CIPHER_PREFIX.length(), stored.length() - CIPHER_SUFFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "数据源密码解密失败，请检查 xuanchen.crypto.datasource-key 是否与加密时一致", e);
        }
    }

    /**
     * 判断入参是否为"不修改密码"语义：空值或回传的掩码占位。
     */
    public static boolean isKeepExisting(String value) {
        return value == null || value.isBlank() || MASK.equals(value.trim());
    }

    private static boolean isCipherText(String value) {
        return value.startsWith(CIPHER_PREFIX) && value.endsWith(CIPHER_SUFFIX)
                && value.length() > CIPHER_PREFIX.length() + CIPHER_SUFFIX.length();
    }

    private static SecretKeySpec requireKey() {
        SecretKeySpec key = keySpec;
        if (key == null) {
            throw new IllegalStateException("数据源加密密钥尚未初始化");
        }
        return key;
    }
}
