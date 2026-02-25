package com.xuanchen.generator.generator.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.generator.generator.entity.CodeGenerator;
import com.xuanchen.generator.generator.service.ICodeGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 控制器 --> 代码生成器
 *
 * @author XuanChen
 * @date 2025-03-04
 */
@RestController
@RequestMapping("/codeGenerator")
public class CodeGeneratorController {
    @Autowired
    private ICodeGeneratorService codeGeneratorService;

    /**
     * 代码生成
     *
     * @param codeGenerator
     * @return
     */
    @RequestMapping(value = "/generator", method = {RequestMethod.GET})
    public Result codeGenerator(CodeGenerator codeGenerator) {
        codeGeneratorService.codeGenerator(codeGenerator);
        return Result.success("生成成功!");
    }

    /**
     * 获取数据库表列表
     *
     * @param codeGenerator
     * @return
     */
    @RequestMapping(value = "/getTableList", method = {RequestMethod.GET})
    public Result getTableList(CodeGenerator codeGenerator) {
        List<Map<String, Object>> list = codeGeneratorService.getTableList(codeGenerator);
        return Result.success(list);
    }

    /**
     * 获取数据库表信息
     *
     * @param codeGenerator
     * @return
     */
    @RequestMapping(value = "/getTableInfo", method = {RequestMethod.GET})
    public Result getTableInfo(CodeGenerator codeGenerator) {
        List<Map<String, Object>> list = codeGeneratorService.getTableInfo(codeGenerator);
        return Result.success(list);
    }
}
