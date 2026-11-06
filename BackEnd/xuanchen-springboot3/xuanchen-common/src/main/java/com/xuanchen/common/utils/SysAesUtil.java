package com.xuanchen.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 统一数据源密码 AES/GCM 加解密（sys_datasource）。
 * 密钥通过配置 {@code xuanchen.sys.encrypt-key} 提供（16/24/32 字节），
 * 未配置时使用内置开发密钥，生产环境务必显式配置。
 * <p>
 * 放 common 模块 → 所有业务模块（report/api/visualization/generator）都能零依赖使用。
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Slf4j
@Component
public class SysAesUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String DEFAULT_KEY = "XuanChen@Report!";

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public SysAesUtil(@Value("${xuanchen.sys.encrypt-key:" + DEFAULT_KEY + "}") String key) {
        if (key == null || key.isBlank() || DEFAULT_KEY.equals(key)) {
            log.warn("[SYS-AES] 正在使用内置开发密钥加密数据源口令，生产环境必须配置 xuanchen.sys.encrypt-key！");
        }
        byte[] keyBytes = normalizeKey(key);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    /** 加密明文，返回 Base64(iv + cipherText) */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("数据源密码加密失败", e);
        }
    }

    /** 解密 Base64(iv + cipherText)，解密失败返回空串（兼容旧调用方） */
    public String decrypt(String cipherText) {
        String plain = decryptOrNull(cipherText);
        return plain == null ? "" : plain;
    }

    /**
     * 解密并返回明文；输入不是合法密文（例如前端传来的明文口令）或密钥不匹配时返回 null。
     * 供 controller 区分「密文」与「明文」，避免对同一口令重复解密。
     */
    public String decryptOrNull(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return null;
        }
        try {
            byte[] data = Base64.getDecoder().decode(cipherText);
            if (data.length <= IV_LENGTH + TAG_LENGTH_BITS / 8) {
                return null;
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, data, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** 归一化为 AES 合法密钥长度（16/24/32 字节）：短则补零，超长则截断 */
    private static byte[] normalizeKey(String key) {
        byte[] bytes = (key == null ? "" : key).getBytes(StandardCharsets.UTF_8);
        int length = bytes.length <= 16 ? 16 : bytes.length <= 24 ? 24 : 32;
        byte[] normalized = new byte[length];
        System.arraycopy(bytes, 0, normalized, 0, Math.min(bytes.length, length));
        return normalized;
    }
}
