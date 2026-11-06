package com.xuanchen.ai.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.ai.kb.dto.AiDocImportDTO;
import com.xuanchen.ai.kb.dto.AiDocTextDTO;
import com.xuanchen.ai.kb.entity.AiDocument;
import com.xuanchen.ai.kb.entity.AiKb;
import com.xuanchen.ai.kb.rag.AiIngestService;
import com.xuanchen.ai.kb.service.IAiDocumentService;
import com.xuanchen.ai.kb.service.IAiKbService;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.service.IFileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 控制器-->AI知识库文档（上传/解析/删除）
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@RestController
@RequestMapping("/ai/kb/doc")
public class AiDocumentController {

    /**
     * 知识库文档统一业务类型：作为 MinIO objectKey 首段（bizType 白名单格式校验通过）
     */
    private static final String BIZ_TYPE = "ai-kb";

    /**
     * 手动录入正文长度上限（字符），防止超大文本一次性打爆向量化
     */
    private static final int TEXT_MAX_CHARS = 200_000;

    /**
     * AI 知识库支持解析的文档扩展名白名单（Apache Tika 可抽取文本并适合向量入库的格式）
     * <p>文件中心允许的图片/压缩包/CAD 等格式虽能上传但无法被 Tika 抽取有效文本，
     * 故在此单独拦截，前端提示与后端严格校验保持一致。</p>
     */
    private static final Set<String> KB_PARSEABLE_EXTS = Set.of(
            "pdf",                                          // PDF
            "doc", "docx",                                  // Word
            "xls", "xlsx",                                  // Excel
            "ppt", "pptx",                                  // PowerPoint
            "txt", "md", "markdown"                         // 纯文本/Markdown
    );

    /**
     * 生成给用户的「支持格式列表」展示文案
     */
    private static final String KB_PARSEABLE_DESC = "pdf、doc、docx、xls、xlsx、ppt、pptx、txt、md";

    @Autowired
    private IAiKbService kbService;
    @Autowired
    private IAiDocumentService documentService;
    @Autowired
    private AiIngestService ingestService;
    @Autowired
    private IFileInfoService fileInfoService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 分页查询知识库下的文档
     */
    @GetMapping("/list")
    public Result<IPage<AiDocument>> list(AiDocument doc,
                                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        if (doc.getKbId() == null || doc.getKbId().isBlank()) {
            return Result.badRequest("缺少知识库ID！");
        }
        return Result.success(documentService.listDoc(new Page<>(pageNo, pageSize), doc));
    }

    /**
     * 上传文档：复用文件中心上传（MinIO/去重/配额/安全校验），随后自动提交异步解析
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("kbId") String kbId,
                                 HttpServletRequest request) {
        Result<String> kbCheck = checkKbUsable(kbId);
        if (kbCheck != null) {
            return kbCheck;
        }
        //0、知识库可解析格式校验（在文件中心收货之前拦截，避免产生孤儿 file_info 记录）
        String ext = extractExt(file.getOriginalFilename());
        Result<String> extCheck = checkParseableExt(ext);
        if (extCheck != null) {
            return extCheck;
        }
        // 内容指纹查重：在文件中心收货之前拦截，避免拒绝后留下孤儿 file_info 记录
        String md5;
        try {
            md5 = DigestUtils.md5DigestAsHex(file.getBytes());
        } catch (IOException e) {
            return Result.error("读取上传文件失败！");
        }
        AiDocument dup = findDuplicate(kbId, md5);
        if (dup != null) {
            return Result.badRequest("上传失败：相同内容的文档「" + dup.getFileName() + "」已存在于本知识库，请勿重复上传！");
        }
        FileInfo info = fileInfoService.upload(file, BIZ_TYPE, authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        AiDocument doc = buildDoc(kbId, info, md5);
        documentService.save(doc);
        // 上传即解析：失败可重试，状态前端轮询
        ingestService.submitParse(doc.getId());
        return Result.success("上传成功，解析中!");
    }

    /**
     * 手动录入知识：正文包装为 txt 复用文件中心字节存储（同一套校验/配额/objectKey），随后自动解析
     */
    @PostMapping("/text")
    public Result<String> text(@RequestBody AiDocTextDTO dto, HttpServletRequest request) {
        if (dto.getKbId() == null || dto.getKbId().isBlank()) {
            return Result.badRequest("缺少知识库ID！");
        }
        Result<String> kbCheck = checkKbUsable(dto.getKbId());
        if (kbCheck != null) {
            return kbCheck;
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            return Result.badRequest("正文内容不能为空！");
        }
        if (dto.getContent().length() > TEXT_MAX_CHARS) {
            return Result.badRequest("正文过长（上限 " + TEXT_MAX_CHARS + " 字符），请拆分为多个片段分次录入！");
        }
        String title = dto.getTitle() == null || dto.getTitle().isBlank()
                ? "手动录入-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                : dto.getTitle().trim();
        if (title.length() > 100) {
            title = title.substring(0, 100);
        }
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        byte[] bytes = dto.getContent().getBytes(StandardCharsets.UTF_8);
        String md5 = DigestUtils.md5DigestAsHex(bytes);
        AiDocument dup = findDuplicate(dto.getKbId(), md5);
        if (dup != null) {
            return Result.badRequest("录入失败：相同内容的文档「" + dup.getFileName() + "」已存在于本知识库，请勿重复录入！");
        }
        FileInfo info = fileInfoService.store(bytes, title + ".txt", BIZ_TYPE, username);
        AiDocument doc = buildDoc(dto.getKbId(), info, md5);
        documentService.save(doc);
        ingestService.submitParse(doc.getId());
        return Result.success("录入成功，解析中!");
    }

    /**
     * 从文件中心导入已有文件：不重复上传，按 file_info.id 直接建关联后解析
     * <p>校验：文件存在且未删除、归属当前用户、同一知识库内不重复导入。</p>
     */
    @PostMapping("/importFromCenter")
    public Result<String> importFromCenter(@RequestBody AiDocImportDTO dto, HttpServletRequest request) {
        if (dto.getKbId() == null || dto.getKbId().isBlank()) {
            return Result.badRequest("缺少知识库ID！");
        }
        Result<String> kbCheck = checkKbUsable(dto.getKbId());
        if (kbCheck != null) {
            return kbCheck;
        }
        if (dto.getFileIds() == null || dto.getFileIds().isEmpty()) {
            return Result.badRequest("请选择要导入的文件！");
        }
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        List<AiDocument> toSave = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String fileId : dto.getFileIds()) {
            FileInfo info = fileInfoService.getById(fileId);
            if (info == null) {
                skipped.add(fileId + "(不存在)");
                continue;
            }
            if (!username.equals(info.getUsername())) {
                skipped.add(info.getOriginalName() + "(仅支持导入本人文件)");
                continue;
            }
            // 知识库可解析格式校验（文件中心的图片/压缩包/CAD 等无法被 Tika 抽取文本）
            String fileExt = info.getExt() == null ? extractExt(info.getOriginalName())
                    : info.getExt().toLowerCase(Locale.ROOT);
            if (!KB_PARSEABLE_EXTS.contains(fileExt)) {
                skipped.add(info.getOriginalName() + "(不支持该格式，仅支持 " + KB_PARSEABLE_DESC + ")");
                continue;
            }
            long dup = documentService.count(new LambdaQueryWrapper<AiDocument>()
                    .eq(AiDocument::getKbId, dto.getKbId())
                    .eq(AiDocument::getFileId, fileId));
            if (dup > 0) {
                skipped.add(info.getOriginalName() + "(已导入过)");
                continue;
            }
            // 内容指纹查重：同一份内容重新上传过（fileId 不同）也拦下
            AiDocument dupByMd5 = findDuplicate(dto.getKbId(), info.getMd5());
            if (dupByMd5 != null) {
                skipped.add(info.getOriginalName() + "(内容与已有文档重复)");
                continue;
            }
            toSave.add(buildDoc(dto.getKbId(), info, info.getMd5()));
        }
        for (AiDocument doc : toSave) {
            documentService.save(doc);
            ingestService.submitParse(doc.getId());
        }
        if (toSave.isEmpty()) {
            return Result.badRequest("没有可导入的文件：" + String.join("、", skipped));
        }
        String msg = "成功导入 " + toSave.size() + " 个文档，解析中!";
        if (!skipped.isEmpty()) {
            msg += " 跳过 " + skipped.size() + " 个：" + String.join("、", skipped);
        }
        return Result.success(msg);
    }

    /**
     * 触发（重新）解析：先清旧向量再重新入库，用于失败重试与调参后重建。
     * <p>卡死看护：parsing 状态超过 10 分钟无进展视为解析线程异常中断
     * （典型场景：IDE Debug 断点挂起异步线程），此时允许强制重新入队。</p>
     */
    @PostMapping("/parse")
    public Result<String> parse(@RequestParam("id") String id) {
        AiDocument doc = documentService.getById(id);
        if (doc == null) {
            return Result.badRequest("文档不存在或已被删除！");
        }
        if ("parsing".equals(doc.getStatus()) && doc.getUpdateTime() != null
                && doc.getUpdateTime().isAfter(LocalDateTime.now().minusMinutes(10))) {
            return Result.badRequest("正在解析中，请耐心等待（大文档为分钟级任务）！");
        }
        ingestService.submitParse(id);
        return Result.success("已提交解析!");
    }

    /**
     * 删除文档（逻辑删除 + 清理向量；MinIO 文件保留在文件中心）
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("id") String id) {
        AiDocument doc = documentService.getById(id);
        if (doc == null) {
            return Result.badRequest("文档不存在或已被删除！");
        }
        ingestService.removeVectors(id);
        documentService.removeById(id);
        return Result.success("删除成功!");
    }

    /**
     * 知识库可用性校验：存在且启用返回 null，否则返回错误 Result
     */
    private Result<String> checkKbUsable(String kbId) {
        AiKb kb = kbService.getById(kbId);
        if (kb == null) {
            return Result.badRequest("知识库不存在或已被删除！");
        }
        if (kb.getStatus() == null || kb.getStatus() != 1) {
            return Result.badRequest("知识库已停用，请先启用再添加文档！");
        }
        return null;
    }

    /**
     * 知识库可解析格式校验：扩展名在白名单内返回 null，否则返回包含支持格式列表的错误 Result
     */
    private Result<String> checkParseableExt(String ext) {
        if (ext == null || ext.isBlank() || !KB_PARSEABLE_EXTS.contains(ext.toLowerCase(Locale.ROOT))) {
            return Result.badRequest("不支持该文档格式（" + (ext == null ? "未知" : ext) + "），仅支持：" + KB_PARSEABLE_DESC);
        }
        return null;
    }

    /**
     * 从文件名中提取小写扩展名（不含点号），文件名无扩展名时返回空串
     */
    private static String extractExt(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 知识库内容查重：同知识库内已有未失败文档的内容指纹（MD5）与本次一致即视为重复。
     * <p>failed 文档放行（视为未入柜，允许重传修复）；跨知识库不去重（各库独立检索范围）。</p>
     */
    private AiDocument findDuplicate(String kbId, String md5) {
        if (md5 == null || md5.isBlank()) {
            return null;
        }
        return documentService.getOne(new LambdaQueryWrapper<AiDocument>()
                .eq(AiDocument::getKbId, kbId)
                .eq(AiDocument::getMd5, md5.toLowerCase())
                .ne(AiDocument::getStatus, "failed")
                .last("LIMIT 1"), false);
    }

    /**
     * 由文件中心元数据构建待入库文档（pending，解析由线程池异步推进）
     */
    private AiDocument buildDoc(String kbId, FileInfo info, String md5) {
        AiDocument doc = new AiDocument();
        doc.setKbId(kbId);
        doc.setFileName(info.getOriginalName());
        doc.setFileId(info.getId());
        doc.setFileType(info.getExt());
        doc.setFileSize(info.getFileSize());
        // 内容指纹（统一小写，与文件中心 md5Hex 口径一致）
        doc.setMd5(md5 == null ? null : md5.toLowerCase());
        doc.setStatus("pending");
        doc.setChunkCount(0);
        return doc;
    }
}
