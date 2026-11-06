package com.xuanchen.common.excel;

import com.xuanchen.common.excel.resolver.ExcelDictResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 导出值翻译注册表（通用、可扩展）。
 * <p>
 * 字段上 {@link com.xuanchen.common.excel.annotation.ExcelDict @ExcelDict} 声明的"翻译类型"
 * 会按解析器链依次翻译，第一个返回非 null 的结果生效：
 * <ol>
 *     <li><b>DB 数据字典</b>：类型即 {@code sys_dict.dict_code}（如 {@code "user_status"}、
 *     {@code "del_flag"}），在【字典管理】界面增删字典项即可改变导出文案，
 *     无需改代码重新部署（带 60 秒缓存）；</li>
 *     <li><b>自定义解析器</b>：业务模块实现 {@link ExcelDictResolver} 并注册为 Spring Bean，
 *     用 {@link org.springframework.core.annotation.Order @Order} 控制优先级
 *     （未显式指定时排在 DB 字典解析器之后）；</li>
 *     <li>都未命中时原样输出编码值（不丢数据）。</li>
 * </ol>
 *
 * <h3>扩展方式</h3>
 * <ul>
 *     <li>有界、可能让运维调整的取值（含 del_flag 这类全系统通用标志）：在字典管理建
 *     dict_code，实体字段直接 {@code @ExcelDict("dict_code")}；</li>
 *     <li>跨表/计算型翻译：实现 {@link ExcelDictResolver} 注册 Bean。</li>
 * </ul>
 * 本类为纯静态工具，不交给 Spring 管理；解析器链由
 * {@link com.xuanchen.common.excel.config.ExcelDictChainRegistrar} 在容器就绪后装配。
 *
 * @author XuanChen
 * @date 2026-09-28
 */
@Slf4j
public final class ExcelDictTranslator {

    private ExcelDictTranslator() {
    }

    /**
     * Spring 容器中的解析器链（按 @Order 升序）；非 Spring 环境（如离线单测）为 null
     */
    private static volatile List<ExcelDictResolver> springResolvers;

    /**
     * 装配/重建解析器链（由 {@link com.xuanchen.common.excel.config.ExcelDictChainRegistrar}
     * 在所有解析器 Bean 就绪后调用）；传 null 则清空链（非 Spring 环境下翻译原样返回编码）。
     * 框架内部装配入口，业务代码通常无需调用。
     */
    public static void rebuildResolvers(List<ExcelDictResolver> resolvers) {
        if (resolvers == null) {
            springResolvers = null;
            return;
        }
        List<ExcelDictResolver> sorted = new ArrayList<>(resolvers);
        AnnotationAwareOrderComparator.sort(sorted);
        springResolvers = Collections.unmodifiableList(sorted);
        log.info("Excel 导出翻译解析器链就绪，共 {} 个: {}", sorted.size(),
                sorted.stream().map(r -> r.getClass().getSimpleName()).toList());
    }

    /**
     * 按类型把编码值翻译为中文文案。供 {@link com.xuanchen.common.excel.converter.ExcelDictConverter}
     * 在每个单元格写入时调用；值为 null 返回 null（由 FesodSheet 按空值策略处理）。
     *
     * @param type 翻译类型（DB 字典编码或自定义解析器约定的类型名）
     * @param code 编码值（Integer/String 等，内部按字符串匹配）
     * @return 中文文案；所有解析器均未命中（或非 Spring 环境链未装配）时原样返回编码字符串
     */
    public static String translate(String type, Object code) {
        if (code == null) {
            return null;
        }
        List<ExcelDictResolver> chain = springResolvers != null ? springResolvers : Collections.emptyList();
        return translateWith(type, code, chain);
    }

    /**
     * 解析器链核心逻辑（独立成静态方法，便于不启动 Spring 容器直接单测链路行为）
     */
    static String translateWith(String type, Object code, List<ExcelDictResolver> resolvers) {
        String codeText = String.valueOf(code);
        for (ExcelDictResolver resolver : resolvers) {
            try {
                String text = resolver.resolve(type, code);
                if (text != null) {
                    return text;
                }
            } catch (Exception e) {
                // 单个解析器异常不影响导出：记录后继续尝试链上的其他来源
                log.warn("Excel 导出翻译解析器执行异常，继续尝试下一个 resolver={}, type={}, code={}",
                        resolver.getClass().getSimpleName(), type, codeText, e);
            }
        }
        return codeText;
    }
}
