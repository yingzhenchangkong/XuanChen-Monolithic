package com.xuanchen.generator.generator.service.impl;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.xuanchen.generator.generator.entity.CodeGenerator;
import com.xuanchen.generator.generator.service.ICodeGeneratorService;
import org.springframework.stereotype.Service;

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
    @Override
    public void codeGenerator(CodeGenerator codeGenerator) {
        String url = "jdbc:mysql://" + codeGenerator.getUrl() + "/" + codeGenerator.getDatabaseName() + "?serverTimezone=GMT%2B8&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false&tinyInt1isBit=true";
        String username = codeGenerator.getUsername();
        String password = codeGenerator.getPassword();
        String outputDir = codeGenerator.getOutputDir();
        String moduleName = codeGenerator.getModuleName();
        String packageName = codeGenerator.getPackageName();
        String packagePath = codeGenerator.getOutputDir() + "/com/xuanchen/" + codeGenerator.getModuleName() + "/" + codeGenerator.getPackageName() + "/mapper/xml";
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
                            builder.entityBuilder()
                                    .enableFileOverride() //覆盖已生成文件
                                    .enableLombok();//开启 lombok
                            builder.controllerBuilder()
                                    .enableFileOverride() //覆盖已生成文件
                                    .enableRestStyle();//开启生成@RestController 控制器
                            builder.serviceBuilder()
                                    .enableFileOverride(); //覆盖已生成文件
                            builder.mapperBuilder()
                                    .enableFileOverride(); //覆盖已生成文件
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
