package com.xuanchen.common.excel.resolver;

import com.xuanchen.common.api.DictApi.mapper.DictApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DB 数据字典翻译解析器（优先级最高）。
 * <p>
 * 翻译类型即 {@code sys_dict.dict_code}：运维在【字典管理】界面维护字典项，
 * 导出自动按最新字典翻译，无需改代码。每个 dictCode 的整组字典项只查一次库并缓存
 * {@link #CACHE_TTL_MS} 毫秒（导出动辄上千行，逐行查库不可接受）；字典中不存在的
 * 类型也会缓存空表（负缓存），避免每行一次 SQL，到期后自然回库刷新。
 * <p>
 * 本解析器未命中（类型不是字典编码 / 字典里没有该值）时返回 null，
 * 由链上的自定义解析器继续兜底；全部未命中时导出原样输出编码。
 *
 * @author XuanChen
 * @date 2026-09-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SysDictExcelDictResolver implements ExcelDictResolver {

    private final DictApiMapper dictApiMapper;

    /**
     * 字典映射缓存有效期：60 秒。字典项在界面变更后最迟 60 秒反映到导出
     */
    private static final long CACHE_TTL_MS = 60_000L;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(long expireAt, Map<String, String> valueToText) {
    }

    @Override
    public String resolve(String type, Object code) {
        return loadMapping(type).get(String.valueOf(code));
    }

    private Map<String, String> loadMapping(String dictCode) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(dictCode);
        if (entry != null && entry.expireAt() > now) {
            return entry.valueToText();
        }
        Map<String, String> mapping = new HashMap<>();
        try {
            List<Map<String, Object>> rows = dictApiMapper.selectDictItemMap(dictCode);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    String value = getStringIgnoreCase(row, "dict_item_value");
                    String text = getStringIgnoreCase(row, "dict_item_text");
                    if (value != null) {
                        mapping.put(value, text);
                    }
                }
            }
        } catch (Exception e) {
            // 字典查询失败（如库抖动）不应阻断导出：本轮不缓存，下轮重试，本次交给后续解析器兜底
            log.warn("导出翻译读取数据字典失败，本轮不缓存 dictCode={}", dictCode, e);
            return Map.of();
        }
        cache.put(dictCode, new CacheEntry(now + CACHE_TTL_MS, Map.copyOf(mapping)));
        return mapping;
    }

    /**
     * JDBC 对 Map 结果键的大小写处理在不同驱动/配置下不一致，做大小写兼容读取
     */
    private String getStringIgnoreCase(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value == null ? null : String.valueOf(value);
    }
}
