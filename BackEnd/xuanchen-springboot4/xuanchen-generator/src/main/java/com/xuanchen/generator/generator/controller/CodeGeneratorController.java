package com.xuanchen.generator.generator.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;
import com.xuanchen.generator.gendatabase.service.IGenDatabaseService;
import com.xuanchen.generator.generator.entity.CodeGenerator;
import com.xuanchen.generator.generator.service.ICodeGeneratorService;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.service.IGenTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制器 --> 代码生成器
 *
 * @author XuanChen
 * @date 2025-03-04
 */
@RestController
@RequestMapping("/codeGenerator")
@PreAuthorize("hasRole('admin')")
public class CodeGeneratorController {
    @Autowired
    private ICodeGeneratorService codeGeneratorService;
    @Autowired
    private IGenTableService genTableService;
    @Autowired
    private IGenDatabaseService genDatabaseService;

    /**
     * 代码生成。
     * <p>
     * 前端在"代码生成器"列表中对某一行触发生成，只需传 gen_table 主键 id；
     * 生成所需的数据源连接信息（主机/端口/库名/账号密码）与输出配置（输出目录/模块名/包名/表名）
     * 均由后端按已保存的 gen_table + gen_database 配置组装，避免连接凭据经前端来回传递。
     *
     * @param id gen_table 主键
     * @return 生成结果
     */
    @GetMapping("/generator")
    public Result codeGenerator(@RequestParam(name = "id", required = true) String id) {
        GenTable genTable = genTableService.getById(id);
        if (genTable == null) {
            throw new BusinessException(400, "表配置不存在，请先保存代码生成配置");
        }
        if (genTable.getDatabaseId() == null || genTable.getDatabaseId().isBlank()) {
            throw new BusinessException(400, "未选择数据源，无法生成");
        }
        GenDatabase genDatabase = genDatabaseService.getById(genTable.getDatabaseId());
        if (genDatabase == null) {
            throw new BusinessException(400, "数据源配置不存在或已删除");
        }
        if (genTable.getTableName() == null || genTable.getTableName().isBlank()) {
            throw new BusinessException(400, "未配置数据库表名，无法生成");
        }
        if (genTable.getOutputDir() == null || genTable.getOutputDir().isBlank()) {
            throw new BusinessException(400, "未配置输出目录，无法生成");
        }
        if (genTable.getModuleName() == null || genTable.getModuleName().isBlank()) {
            throw new BusinessException(400, "未配置模块名，无法生成");
        }
        if (genTable.getPackageName() == null || genTable.getPackageName().isBlank()) {
            throw new BusinessException(400, "未配置包名，无法生成");
        }

        CodeGenerator codeGenerator = new CodeGenerator();
        // CodeGenerator.url 承载 "主机:端口"，Service 内再拼完整 jdbc url
        codeGenerator.setUrl(genDatabase.getHost() + ":" + genDatabase.getPort());
        codeGenerator.setHost(genDatabase.getHost());
        codeGenerator.setPort(genDatabase.getPort());
        codeGenerator.setDatabaseName(genDatabase.getDbName());
        codeGenerator.setUsername(genDatabase.getUserName());
        codeGenerator.setPassword(genDatabase.getPassword());
        codeGenerator.setOutputDir(genTable.getOutputDir());
        codeGenerator.setModuleName(genTable.getModuleName());
        codeGenerator.setPackageName(genTable.getPackageName());
        codeGenerator.setTableName(genTable.getTableName());

        codeGeneratorService.codeGenerator(codeGenerator);
        return Result.success("生成成功!");
    }
}
