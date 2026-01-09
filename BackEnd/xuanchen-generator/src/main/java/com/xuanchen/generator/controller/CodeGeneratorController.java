package com.xuanchen.generator.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.xuanchen.common.entity.Result;
import com.xuanchen.generator.entity.CodeGenerator;
import com.xuanchen.generator.utils.DBUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * 控制器 --> 代码生成器
 *
 * @author XuanChen
 * @date 2025-03-04
 */
@RestController
@RequestMapping("/codeGenerator")
public class CodeGeneratorController {

    /**
     * 代码生成
     *
     * @param codeGenerator
     * @return
     */
    @RequestMapping(value = "/generator", method = {RequestMethod.GET})
    public Result codeGenerator(CodeGenerator codeGenerator) {
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
        DruidDataSource druidDataSource = new DruidDataSource();
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection conn = DBUtil.getConnection(codeGenerator, druidDataSource);
            String strSql = "SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()";
            PreparedStatement preparedStatement = conn.prepareStatement(strSql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("tableName", resultSet.getString("TABLE_NAME"));//表名
                map.put("tableComment", resultSet.getString("TABLE_COMMENT"));//表注释
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            druidDataSource.close();
        }
        return Result.success(list);
    }

    @RequestMapping(value = "/getTableInfo", method = {RequestMethod.GET})
    public Result getTableInfo(CodeGenerator codeGenerator) {
        DruidDataSource druidDataSource = new DruidDataSource();
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection conn = DBUtil.getConnection(codeGenerator, druidDataSource);
            String strSql = "SELECT COLUMN_NAME,COLUMN_COMMENT,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,IS_NULLABLE,COLUMN_DEFAULT,COLUMN_KEY,"
                    + "EXTRA FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + codeGenerator.getDatabaseName() + "' AND TABLE_NAME = '"
                    + codeGenerator.getTableName() + "';";
            PreparedStatement preparedStatement = conn.prepareStatement(strSql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("columnName", resultSet.getString("COLUMN_NAME"));//字段名
                map.put("columnComment", resultSet.getString("COLUMN_COMMENT"));//字段注释
                map.put("dataType", resultSet.getString("DATA_TYPE"));//字段类型
                map.put("characterMaximumLength", resultSet.getString("CHARACTER_MAXIMUM_LENGTH"));//字段最大长度
                map.put("isNullable", resultSet.getString("IS_NULLABLE"));//字段是否允许为NULL
                map.put("columnDefault", resultSet.getString("COLUMN_DEFAULT"));//字段默认值
                map.put("columnKey", resultSet.getString("COLUMN_KEY"));//键类型
                map.put("extra", resultSet.getString("EXTRA"));//扩展信息
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            druidDataSource.close();
        }
        return Result.success(list);
    }
}
