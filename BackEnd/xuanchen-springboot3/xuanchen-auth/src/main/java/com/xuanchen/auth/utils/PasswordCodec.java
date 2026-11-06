package com.xuanchen.auth.utils;

import org.apache.shiro.crypto.hash.AbstractCryptHash;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashProvider;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.HashService;
import org.apache.shiro.crypto.hash.HashSpi;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.lang.util.ByteSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 口令编解码器：BCrypt 慢哈希 + 历史 SHA-512(1024轮)+盐 双栈兼容。
 * <p>
 * 新口令一律使用 BCrypt（cost=10，自带随机盐，输出 $2a$ 前缀、60 字符）；
 * BCrypt 能力来自 Shiro 密码学扩展 shiro-hashes-bcrypt（HashSpi SPI 自动发现，
 * 底层 BouncyCastle OpenBSDBCrypt），与原 spring-security-crypto 的
 * BCryptPasswordEncoder 输出格式互通，存量哈希无需迁移。
 * <p>
 * 库内历史 SHA-512 十六进制哈希仍可校验，登录校验成功后由调用方触发一次性升级（rehash on login），
 * 用户无感知地逐步淘汰快哈希，避免"一刀切改哈希导致全部存量账号无法登录"。
 *
 * @author XuanChen
 * @date 2026-09-30
 */
@Component
public class PasswordCodec {

    /**
     * 历史 SHA-512 方案的迭代次数（与旧实现保持一致，仅用于存量哈希校验）
     */
    private static final int LEGACY_ITERATIONS = 1024;

    /**
     * BCrypt 算法名（Shiro HashSpi 注册名）：2a 版本与 Spring BCryptPasswordEncoder
     * 的历史输出前缀保持一致；默认 cost=10，与原实现相同
     */
    private static final String BCRYPT_ALGORITHM = "bcrypt2a";

    private final HashService hashService = new DefaultHashService();

    /**
     * 使用 BCrypt 对明文口令做单向哈希（每次调用内置随机盐，同一明文输出不同）
     *
     * @param rawPassword 明文口令
     * @return $2a$ 开头的 BCrypt 哈希
     */
    public String encode(String rawPassword) {
        Hash hash = hashService.computeHash(
                new HashRequest.Builder()
                        .setSource(rawPassword)
                        // 请求级算法名：不依赖 ConfigurableHashService 的 setter，
                        // 对 HashService 接口类型即可调用
                        .setAlgorithmName(BCRYPT_ALGORITHM)
                        .build());
        if (hash == null) {
            throw new IllegalArgumentException("口令哈希失败：输入为空");
        }
        return ((AbstractCryptHash) hash).formatToCryptString();
    }

    /**
     * 校验明文口令与库内哈希是否匹配：BCrypt 哈希走 BCrypt 比对；
     * 其余按历史 SHA-512(password, salt, 1024) 十六进制比对（常量时间比较，防计时侧信道）。
     *
     * @param rawPassword 明文口令
     * @param storedHash  库内密码哈希
     * @param salt        历史 SHA-512 方案的盐；BCrypt 哈希忽略（其盐内嵌在哈希串中）
     * @return true 匹配
     */
    public boolean matches(String rawPassword, String storedHash, String salt) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            // 按 BCrypt 前缀解析存量哈希（$2a$/$2b$/$2y$ 均受支持），
            // 版本/盐/cost 全部取自哈希串本身，不依赖当前默认配置
            HashSpi spi = HashProvider.getByAlgorithmName(BCRYPT_ALGORITHM)
                    .orElseThrow(() -> new IllegalStateException(
                            "BCrypt 支持模块 shiro-hashes-bcrypt 未在 classpath 上"));
            try {
                Hash stored = spi.fromString(storedHash);
                return stored.matchesPassword(ByteSource.Util.bytes(rawPassword));
            } catch (IllegalArgumentException e) {
                // 库内哈希不是合法的 BCrypt crypt 串：按不匹配处理，避免脏数据导致登录报 500
                return false;
            }
        }
        //历史账号：盐缺失则无法重算，直接失败（绝不退回无盐哈希之类的弱化校验）
        if (salt == null || salt.isEmpty()) {
            return false;
        }
        String legacyHex = new Sha512Hash(rawPassword, salt, LEGACY_ITERATIONS).toHex();
        return MessageDigest.isEqual(
                legacyHex.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 库内哈希是否已是 BCrypt（$2a$/$2b$/$2y$ 前缀）
     */
    public boolean isBcrypt(String storedHash) {
        return storedHash != null && storedHash.startsWith("$2");
    }
}
