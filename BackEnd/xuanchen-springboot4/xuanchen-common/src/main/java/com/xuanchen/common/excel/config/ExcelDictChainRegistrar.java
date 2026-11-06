package com.xuanchen.common.excel.config;

import com.xuanchen.common.excel.ExcelDictTranslator;
import com.xuanchen.common.excel.resolver.ExcelDictResolver;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Excel 导出翻译链装配：Spring 启动时把所有 {@link ExcelDictResolver} Bean
 * （DB 字典解析器及各业务模块自定义解析器）收集排序后交给
 * {@link ExcelDictTranslator}。fesod 的转换器由框架实例化、不经过 Spring，
 * 因此通过静态注册表间接使用这条链。
 *
 * @author XuanChen
 * @date 2026-09-28
 */
@Configuration
public class ExcelDictChainRegistrar {

    /**
     * 构造器注入全部解析器（Spring 保证依赖 Bean 先就绪），随即装配静态链
     */
    public ExcelDictChainRegistrar(List<ExcelDictResolver> resolvers) {
        ExcelDictTranslator.rebuildResolvers(resolvers);
    }
}
