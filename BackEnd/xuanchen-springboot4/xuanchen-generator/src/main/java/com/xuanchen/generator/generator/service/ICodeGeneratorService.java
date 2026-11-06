package com.xuanchen.generator.generator.service;

import com.xuanchen.generator.generator.model.GenPreviewFile;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Service接口-->代码生成器
 * <p>
 * 生成所需的数据源连接信息（主机/端口/库名/账号密码）与输出配置（输出目录/模块名/包名/表名）
 * 均由后端按已保存的 gen_table + sys_datasource 配置组装，连接凭据不经前端来回传递。
 *
 * @author XuanChen
 * @date 2026-02-16
 */
public interface ICodeGeneratorService {

    /**
     * 按已保存的表配置生成代码，落盘到 gen_table.output_dir。
     * <p>
     * 字段界面配置取自 gen_table_column（未配置时回退为按物理表元数据推断默认值），
     * 生成完整 CRUD 后端（含查询条件拼装）+ 前端页面（查询区/表格列/表单按字段配置渲染）
     * + 菜单权限 SQL；配置了主子表时一并生成子表后端与主表弹窗内的子表维护区块。
     *
     * @param id gen_table 主键
     * @return 本次实际写出的文件（相对输出根目录的路径）
     */
    List<String> codeGeneratorByTableId(String id);

    /**
     * 按已保存的表配置生成代码并打包为 zip 流式下载（产物含 menu.sql）。
     * <p>
     * 生成在临时目录完成，响应结束后清理，不写入用户配置的输出目录，
     * 适合服务器与开发机分离的场景。
     *
     * @param id gen_table 主键
     */
    void downloadZipByTableId(String id, HttpServletResponse response);

    /**
     * 生成预览：在临时目录完成生成并返回文件列表与内容，不写入用户配置的输出目录。
     * <p>
     * 用于「先看再生成」：确认路径、类名、页面结构无误后再落盘，避免生成完才发现目标目录不对。
     *
     * @param id gen_table 主键
     * @return 预览文件（相对临时根目录的路径 + 文本内容）
     */
    List<GenPreviewFile> previewByTableId(String id);
}
