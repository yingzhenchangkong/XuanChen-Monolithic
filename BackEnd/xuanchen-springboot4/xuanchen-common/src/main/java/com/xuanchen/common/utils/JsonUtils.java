package com.xuanchen.common.utils;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 反序列化为泛型容器（List/Map 等复杂类型）
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    public static Map<String, Object> parseToMap(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    public static JsonNode parseToJsonNode(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return MAPPER.convertValue(fromValue, toValueType);
    }
}
