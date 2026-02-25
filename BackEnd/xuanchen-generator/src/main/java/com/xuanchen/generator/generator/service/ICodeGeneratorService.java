package com.xuanchen.generator.generator.service;

import com.xuanchen.generator.generator.entity.CodeGenerator;

import java.util.List;
import java.util.Map;

/**
 * Service接口-->代码生成器
 *
 * @author XuanChen
 * @date 2026-02-16
 */
public interface ICodeGeneratorService {
    List<Map<String, Object>> getTableList(CodeGenerator codeGenerator);

    List<Map<String, Object>> getTableInfo(CodeGenerator codeGenerator);

    void codeGenerator(CodeGenerator codeGenerator);
}
