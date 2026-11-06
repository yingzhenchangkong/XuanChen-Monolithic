package com.xuanchen.api.engine;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上下文模板取值工具
 * 表达式：{{路径}}，路径以 . 分隔，数字段作为 List 下标（如 rows.0.user_name）。
 * - 整串恰为一个 {{expr}} 时返回原类型值（数字/布尔/对象不退化为字符串）
 * - 否则将串内各 {{expr}} 求值后以字符串拼接
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public final class TemplateEval {

    private static final Pattern EXPR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    private TemplateEval() {
    }

    /**
     * 深度求值：Map/List 结构中所有字符串值里的 {{expr}} 替换为上下文值
     */
    @SuppressWarnings("unchecked")
    public static Object evalDeep(Object value, Map<String, Object> context) {
        if (value instanceof String s) {
            return evalString(s, context);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), evalDeep(e.getValue(), context));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            java.util.List<Object> out = new java.util.ArrayList<>();
            for (Object item : list) {
                out.add(evalDeep(item, context));
            }
            return out;
        }
        return value;
    }

    /**
     * 字符串求值
     */
    public static Object evalString(String s, Map<String, Object> context) {
        Matcher whole = Pattern.compile("^\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}$").matcher(s.trim());
        if (whole.matches()) {
            return getByPath(context, whole.group(1));
        }
        Matcher m = EXPR.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object val = getByPath(context, m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : String.valueOf(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 按路径取上下文值：a.b.0.c
     */
    public static Object getByPath(Object root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        Object cur = root;
        for (String seg : path.split("\\.")) {
            if (cur == null) {
                return null;
            }
            if (cur instanceof Map<?, ?> map) {
                cur = map.get(seg);
            } else if (cur instanceof List<?> list) {
                try {
                    cur = list.get(Integer.parseInt(seg));
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return cur;
    }
}
