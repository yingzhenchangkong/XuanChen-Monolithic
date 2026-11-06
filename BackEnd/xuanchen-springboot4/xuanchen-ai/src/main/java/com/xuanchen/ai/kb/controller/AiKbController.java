package com.xuanchen.ai.kb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.ai.config.AiProperties;
import com.xuanchen.ai.kb.entity.AiDocument;
import com.xuanchen.ai.kb.entity.AiKb;
import com.xuanchen.ai.kb.rag.AiIngestService;
import com.xuanchen.ai.kb.service.IAiDocumentService;
import com.xuanchen.ai.kb.service.IAiKbService;
import com.xuanchen.common.entity.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->AI知识库
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@RestController
@RequestMapping("/ai/kb")
public class AiKbController {

    @Autowired
    private IAiKbService kbService;
    @Autowired
    private IAiDocumentService documentService;
    @Autowired
    private AiIngestService ingestService;
    @Autowired
    private AiProperties aiProperties;

    /**
     * 分页查询知识库
     */
    @GetMapping("/list")
    public Result<IPage<AiKb>> list(AiKb kb,
                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(kbService.listKb(new Page<>(pageNo, pageSize), kb));
    }

    /**
     * 新增知识库（向量模型/分块参数留空时取全局配置默认值）
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody AiKb kb) {
        if (kb.getKbName() == null || kb.getKbName().isBlank()) {
            return Result.badRequest("知识库名称不能为空！");
        }
        if (kbService.ifExistsName(kb)) {
            return Result.badRequest("知识库名称已存在！");
        }
        if (kb.getEmbedModel() == null || kb.getEmbedModel().isBlank()) {
            kb.setEmbedModel(aiProperties.getEmbed().getModel());
        }
        if (kb.getChunkSize() == null || kb.getChunkSize() <= 0) {
            kb.setChunkSize(aiProperties.getRag().getChunkSize());
        }
        if (kb.getChunkOverlap() == null || kb.getChunkOverlap() < 0) {
            kb.setChunkOverlap(aiProperties.getRag().getChunkOverlap());
        }
        kb.setStatus(kb.getStatus() == null ? 1 : kb.getStatus());
        kbService.save(kb);
        return Result.success("添加成功!");
    }

    /**
     * 编辑知识库（含启停切换）
     */
    @PostMapping("/edit")
    public Result<String> edit(@RequestBody AiKb kb) {
        if (kb.getId() == null || kb.getId().isBlank()) {
            return Result.badRequest("缺少知识库ID！");
        }
        if (kb.getKbName() == null || kb.getKbName().isBlank()) {
            return Result.badRequest("知识库名称不能为空！");
        }
        if (kbService.ifExistsName(kb)) {
            return Result.badRequest("知识库名称已存在！");
        }
        AiKb db = kbService.getById(kb.getId());
        if (db == null) {
            return Result.badRequest("知识库不存在或已被删除！");
        }
        kbService.updateById(kb);
        return Result.success("修改成功!");
    }

    /**
     * 删除知识库（逻辑删除，并清理其下全部文档向量）
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        AiKb db = kbService.getById(id);
        if (db == null) {
            return Result.badRequest("知识库不存在或已被删除！");
        }
        // 先清向量（物理），再逻辑删除；文档行随知识库保留在回收站，恢复后可重新解析
        List<AiDocument> docs = documentService.list(
                new QueryWrapper<AiDocument>().eq("kb_id", id).select("id"));
        for (AiDocument doc : docs) {
            ingestService.removeVectors(doc.getId());
        }
        kbService.removeById(id);
        return Result.success("删除成功!");
    }

    /**
     * 批量删除知识库
     */
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        for (String id : idList) {
            Result<String> single = delete(id);
            if (single.getCode() != 200) {
                return single;
            }
        }
        return Result.success("批量删除成功!");
    }

    /**
     * 启停切换（状态列开关，body: {id, status}）
     */
    @PostMapping("/changeStatus")
    public Result<String> changeStatus(@RequestBody AiKb kb) {
        if (kb.getId() == null || kb.getId().isBlank() || kb.getStatus() == null) {
            return Result.badRequest("缺少知识库ID或目标状态！");
        }
        AiKb db = new AiKb();
        db.setId(kb.getId());
        db.setStatus(kb.getStatus());
        kbService.updateById(db);
        return Result.success(kb.getStatus() == 1 ? "启用成功!" : "停用成功!");
    }

    // ==================== 回收站（del_flag=1，走 XML 手写 SQL 绕开 @TableLogic） ====================

    /**
     * 回收站分页
     */
    @GetMapping("/listRecycleBin")
    public Result<IPage<AiKb>> listRecycleBin(AiKb kb,
                                              @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(kbService.listRecycleBin(new Page<>(pageNo, pageSize), kb));
    }

    /**
     * 回收站还原（单条，PUT body: {id}）
     */
    @PutMapping("/revertRecycleBin")
    public Result<String> revertRecycleBin(@RequestBody Map<String, String> map) {
        kbService.revertRecycleBin(map.get("id"));
        return Result.success("还原成功!");
    }

    /**
     * 回收站还原（批量，PUT body: {ids}）
     */
    @PutMapping("/revertRecycleBinBatch")
    public Result<String> revertRecycleBinBatch(@RequestBody Map<String, String> map) {
        kbService.revertRecycleBin(map.get("ids"));
        return Result.success("批量还原成功!");
    }

    /**
     * 回收站彻底删除（单条，级联删除文档行与向量）
     */
    @DeleteMapping("/deleteRecycleBin")
    public Result<String> deleteRecycleBin(@RequestParam("id") String id) {
        kbService.deleteRecycleBin(id);
        return Result.success("删除成功!");
    }

    /**
     * 回收站彻底删除（批量）
     */
    @DeleteMapping("/deleteRecycleBinBatch")
    public Result<String> deleteRecycleBinBatch(@RequestParam("ids") String ids) {
        kbService.deleteRecycleBin(ids);
        return Result.success("批量删除成功!");
    }
}
