package com.xuanchen.common.excel.resolver;

/**
 * Excel 导出值翻译解析器（SPI）。
 * <p>
 * 一个解析器代表一种"编码 -> 中文文案"的来源。所有 Spring 容器中的实现
 * 会被 {@link com.xuanchen.common.excel.ExcelDictTranslator} 自动收集，
 * 按 {@link org.springframework.core.annotation.Order @Order} 排序后依次尝试，
 * 第一个返回非 null 的结果生效。
 * <p>
 * 业务模块要接入自己的翻译来源（如 user_id 翻译成用户名、跨表拼接等）时，
 * 只需实现本接口并注册为 Spring Bean，无需修改 common 与其他模块：
 * <pre>{@code
 * @Component
 * @Order(Ordered.HIGHEST_PRECEDENCE + 20)
 * public class UserNameResolver implements ExcelDictResolver {
 *     public String resolve(String type, Object code) {
 *         if (!"sys_user_name".equals(type)) return null; // 不是自己负责的类型要放行
 *         return sysAccountService.getNickNameById(String.valueOf(code));
 *     }
 * }
 * }</pre>
 *
 * @author XuanChen
 * @date 2026-09-28
 */
@FunctionalInterface
public interface ExcelDictResolver {
    /**
     * 翻译编码值。
     *
     * @param type 字段上 {@link com.xuanchen.common.excel.annotation.ExcelDict#value()} 声明的翻译类型
     * @param code 单元格原始编码值（不会为 null）
     * @return 中文文案；返回 null 表示本解析器不处理/未命中，交给链上的下一个解析器
     */
    String resolve(String type, Object code);
}
