package com.xuanchen.generator.generator.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.generator.generator.model.GenPreviewFile;
import com.xuanchen.generator.generator.service.ICodeGeneratorService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 控制器 --> 代码生成
 * <p>
 * 前端在"代码生成器"列表中对某一行触发，只需传 gen_table 主键 id；
 * 生成所需的数据源连接信息与输出配置均由后端按已保存的 gen_table + sys_datasource
 * 配置组装，避免连接凭据经前端来回传递。
 *
 * @author XuanChen
 * @date 2025-03-04
 */
@RestController
@RequestMapping("/codeGenerator")
@RequiresRoles("admin")
public class CodeGeneratorController {
    @Autowired
    private ICodeGeneratorService codeGeneratorService;

    /**
     * 代码生成（落盘到 gen_table.output_dir 配置的目录）。
     *
     * @param id gen_table 主键
     * @return 本次实际生成的相对文件路径列表
     */
    @GetMapping("/generator")
    public Result<List<String>> codeGenerator(@RequestParam(name = "id", required = true) String id) {
        List<String> files = codeGeneratorService.codeGeneratorByTableId(id);
        return Result.success(files);
    }

    /**
     * 代码生成并打包 zip 下载（产物含 menu.sql 菜单权限 SQL）。
     * <p>
     * 生成在服务器临时目录完成并即时清理，不占用输出目录白名单，
     * 适合服务器与开发机分离、无法直接读取服务器磁盘的场景。
     *
     * @param id gen_table 主键
     */
    @GetMapping("/downloadZip")
    public void downloadZip(@RequestParam(name = "id", required = true) String id,
                            HttpServletResponse response) {
        codeGeneratorService.downloadZipByTableId(id, response);
    }

    /**
     * 生成预览：在服务器临时目录生成并返回文件清单与内容，不写入用户配置的输出目录。
     *
     * @param id gen_table 主键
     * @return 预览文件列表
     */
    @GetMapping("/preview")
    public Result<List<GenPreviewFile>> preview(@RequestParam(name = "id", required = true) String id) {
        return Result.success(codeGeneratorService.previewByTableId(id));
    }
}
