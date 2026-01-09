package com.xuanchen.common.utils;

/**
 * 工具类-->字符串
 *
 * @author XuanChen
 * @date 2025-03-12
 */
public class StringUtil {
    /**
     * 判断字符串是否为空或NULL
     *
     * @param object
     * @return
     */
    public static boolean isEmpty(Object object) {
        if (object == null) {
            return (true);
        }
        if ("".equals(object)) {
            return (true);
        }
        return "null".equals(object);
    }

    /**
     * 判断字符串是否不为空或NULL
     *
     * @param object
     * @return
     */
    public static boolean isNotEmpty(Object object) {
        return object != null && !"".equals(object) && !object.equals("null");
    }
}
