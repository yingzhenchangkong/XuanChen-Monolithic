package com.xuanchen.generator.generator.service.impl;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.xuanchen.generator.generator.entity.CodeGenerator;
import com.xuanchen.generator.generator.security.GeneratorSafetyValidator;
import com.xuanchen.generator.generator.service.ICodeGeneratorService;
import com.xuanchen.generator.utils.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service接口实现类-->代码生成器
 *
 * @author XuanChen
 * @date 2026-02-16
 */
@Service
public class CodeGeneratorServiceImpl implements ICodeGeneratorService {
    @Autowired
    private GeneratorSafetyValidator generatorSafetyValidator;

    @Override
    public void codeGenerator(CodeGenerator codeGenerator) {
        // 统一走 DBUtil：host/port/dbName 白名单校验 + 固定连接属性，防止库名注入 JDBC 属性
        String url = DBUtil.buildMySqlUrl(codeGenerator.getHost(), codeGenerator.getPort(),
                codeGenerator.getDatabaseName());
        String username = codeGenerator.getUsername();
        String password = codeGenerator.getPassword();
        String outputDir = codeGenerator.getOutputDir();
        String moduleName = codeGenerator.getModuleName();
        String packageName = codeGenerator.getPackageName();
        // 生成前再次强制校验：保存入口的校验可被绕过（历史脏数据/直接写库），落盘前必须兜底，
        // 杜绝 moduleName/packageName 携带 ../ 或 outputDir 指向服务器任意路径造成的文件覆盖
        generatorSafetyValidator.validateOutputConfig(outputDir, moduleName, packageName);
        generatorSafetyValidator.validateTableName(codeGenerator.getTableName());
        String packagePath = outputDir + "/com/xuanchen/" + moduleName + "/" + packageName + "/mapper/xml";
        // mapper xml 的落盘路径为手工拼接，单独对白名单做一次 containment 确认
        generatorSafetyValidator.assertWithinRoots(Paths.get(packagePath));
        boolean allowOverride = generatorSafetyValidator.isFileOverrideAllowed();
        String tableName = codeGenerator.getTableName();
        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("XuanChen") //设置作者
                            .outputDir(outputDir)//指定输出目录
                            .commentDate("yyyy-MM-dd");
                })
                .packageConfig(builder ->
                        builder.parent("com.xuanchen." + moduleName) //设置父包名
                                .moduleName(packageName) //设置父包模块名
                                .pathInfo(Collections.singletonMap(OutputFile.xml, packagePath)) //设置 Mapper XML 文件生成路径
                )
                .strategyConfig(builder -> {
                            builder.addInclude(tableName);//设置需要生成的表名
                            // 默认关闭文件覆盖（已存在的文件跳过），仅在管理员显式开启
                            // xuanchen.generator.allow-file-override 时才允许覆盖，防止越权改写既有文件
                            var entityBuilder = builder.entityBuilder().enableLombok();
                            var controllerBuilder = builder.controllerBuilder().enableRestStyle();
                            var serviceBuilder = builder.serviceBuilder();
                            var mapperBuilder = builder.mapperBuilder();
                            if (allowOverride) {
                                entityBuilder.enableFileOverride();
                                controllerBuilder.enableFileOverride();
                                serviceBuilder.enableFileOverride();
                                mapperBuilder.enableFileOverride();
                            }
                        }

                )
                .injectionConfig(injectionConfig -> {
                    Map<String, Object> customMap = new HashMap<>();
                    customMap.put("vue", "vue1234");
                    injectionConfig.customMap(customMap);
                    injectionConfig.customFile(
                            new CustomFile.Builder()
                                    .fileName("Index.vue")//文件名称
                                    .templatePath("templates/Index.vue.ftl")//指定生成模板路径
                                    .packageName("vue")//包名
                                    .build()
                    );
                    injectionConfig.customFile(
                            new CustomFile.Builder()
                                    .fileName("Operation.vue")//文件名称
                                    .templatePath("templates/Modal.vue.ftl")//指定生成模板路径
                                    .packageName("vue/modal")//包名
                                    .build()
                    );
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();//执行生成
    }
}
