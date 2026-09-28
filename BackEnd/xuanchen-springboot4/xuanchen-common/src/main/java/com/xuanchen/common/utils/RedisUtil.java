package com.xuanchen.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 工具类-->Redis
 *
 * @author XuanChen
 * @date 2025-03-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {
    /**
     * SCAN 单批建议返回数量（仅为游标迭代提示值，非硬限制）
     */
    private static final long SCAN_BATCH_SIZE = 500L;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取缓存
     *
     * @param key
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 放入缓存
     *
     * @param key   键
     * @param value 值
     * @return true 成功 false 失败
     */
    public boolean set(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception ex) {
            log.error("Redis set 失败, key={}", key, ex);
            return false;
        }
    }

    /**
     * 放入缓存并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true 成功 false 失败
     */
    public boolean set(String key, String value, long time) {
        try {
            if (time > 0) {
                stringRedisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception ex) {
            log.error("Redis set 失败, key={}, time={}s", key, time, ex);
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值或多个
     */
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                stringRedisTemplate.delete(key[0]);
            } else {
                stringRedisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /**
     * 固定窗口原子计数 Lua：
     * INCR 与"首次计数时 EXPIRE"在同一脚本内原子完成。
     * 不能拆成 Java 侧 get-then-set/incr+expire 两条命令——并发请求会互相覆盖计数，
     * 且每次都续期 TTL 会让窗口被无限顺延，限流形同虚设。
     * KEYS[1]=计数键，ARGV[1]=窗口秒数；返回自增后的计数
     */
    private static final DefaultRedisScript<Long> INCR_FIXED_WINDOW_SCRIPT;

    static {
        INCR_FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>();
        INCR_FIXED_WINDOW_SCRIPT.setScriptText(
                "local current = redis.call('INCR', KEYS[1]); "
                + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; "
                + "return current;");
        INCR_FIXED_WINDOW_SCRIPT.setResultType(Long.class);
    }

    /**
     * 固定窗口原子自增；键首次出现（自增结果为 1）时在同一 Lua 脚本内设置过期时间，
     * 窗口长度固定，后续计数不再续期。用于滑块验证限流、登录失败计数等并发场景。
     *
     * @param key     键
     * @param seconds 固定窗口长度（秒），仅首次计数时生效
     * @return 自增后的值；异常返回 0
     */
    public long incrWithExpire(String key, long seconds) {
        try {
            Long value = stringRedisTemplate.execute(
                    INCR_FIXED_WINDOW_SCRIPT, List.of(key), String.valueOf(seconds));
            return value == null ? 0L : value;
        } catch (Exception ex) {
            log.error("Redis incrWithExpire 失败, key={}", key, ex);
            return 0L;
        }
    }

    /**
     * 基于游标 SCAN 获取匹配的键。
     * <p>
     * 生产环境禁止使用 KEYS 命令：KEYS 是 O(N) 阻塞命令，键量大时会长时间占用 Redis 主线程，
     * 导致全实例命令排队、业务请求级联超时；SCAN 采用游标分批迭代，每次只扫描少量键，
     * 对线上流量无明显冲击（代价是迭代期间结果集可能包含过期/重复的弱一致性视图，
     * 本类调用方均为会话清理/监控展示，可接受）。
     *
     * @param pattern 匹配模式（如 "prefix:*"）
     * @return 匹配的键集合；pattern 为空或执行异常时返回空集合（不返回 null）
     */
    public Set<String> scanKeys(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return Collections.emptySet();
        }
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH_SIZE).build();
        try {
            return stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> keys = new HashSet<>();
                // 游标必须在连接回调内消费完毕并关闭：连接归还后再遍历游标会失效
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return keys;
            });
        } catch (Exception ex) {
            log.error("Redis SCAN 失败, pattern={}", pattern, ex);
            return Collections.emptySet();
        }
    }
}
