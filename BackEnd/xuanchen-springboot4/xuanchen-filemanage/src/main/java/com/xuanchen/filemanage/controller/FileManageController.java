package com.xuanchen.filemanage.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.FileSecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 控制器 --> 文件管理
 *
 * @author XuanChen
 * @date 2025-04-21
 */
@RestController
@RequestMapping("/filemanage")
public class FileManageController {
    @Value("${xuanchen.path.upload}")
    private String uploadPath;

    /**
     * 上传文件大小上限（DataSize 格式，如 5MB）
     */
    @Value("${xuanchen.upload.max-size:5MB}")
    private String maxSize;

    /**
     * 图片扩展名 -> Content-Type 兜底映射（部分环境 Files.probeContentType 返回 null）
     */
    private static final Map<String, String> IMAGE_CONTENT_TYPES = new HashMap<>();

    static {
        IMAGE_CONTENT_TYPES.put("jpg", "image/jpeg");
        IMAGE_CONTENT_TYPES.put("jpeg", "image/jpeg");
        IMAGE_CONTENT_TYPES.put("png", "image/png");
        IMAGE_CONTENT_TYPES.put("gif", "image/gif");
        IMAGE_CONTENT_TYPES.put("webp", "image/webp");
        IMAGE_CONTENT_TYPES.put("bmp", "image/bmp");
    }

    /**
     * 通用 文件上传（当前业务仅允许图片：头像）
     * 安全约束：
     * 1. customPath 逐段白名单校验，拒绝绝对路径/盘符/“..”/编码穿越/非法字符；
     * 2. 原始文件名剥离客户端路径，强制要求主名+扩展名，扩展名限图片白名单；
     * 3. 服务端大小校验；
     * 4. 文件头魔数识别真实类型，必须与扩展名一致，防止改后缀绕过；
     * 5. 存储文件名由服务端随机生成，落盘路径必须包含在上传根目录之内。
     *
     * @param request
     * @return 入库用相对路径
     */
    @PostMapping(value = "/upload")
    public Result<String> upload(HttpServletRequest request) {
        //1、校验业务子目录
        String subPath;
        try {
            subPath = FileSecurityUtil.sanitizeSubPath(request.getParameter("customPath"));
        } catch (IllegalArgumentException e) {
            return Result.badRequest("上传目录非法：" + e.getMessage());
        }

        //2、获取并校验上传文件对象
        MultipartFile multipartFile = null;
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            multipartFile = multipartRequest.getFile("file");
        }
        if (multipartFile == null || multipartFile.isEmpty()) {
            return Result.badRequest("上传文件不能为空！");
        }

        //3、文件名与扩展名白名单
        String ext;
        String originalFilename;
        try {
            originalFilename = FileSecurityUtil.stripClientPath(multipartFile.getOriginalFilename());
            ext = FileSecurityUtil.normalizeImageExt(FileSecurityUtil.extensionOf(originalFilename));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }

        //4、文件大小校验
        long maxBytes = DataSize.parse(maxSize).toBytes();
        if (multipartFile.getSize() > maxBytes) {
            return Result.badRequest("文件大小超出限制（最大 " + maxSize + "）！");
        }

        //5、读取内容并用魔数识别真实类型
        byte[] fileBytes;
        try {
            fileBytes = multipartFile.getBytes();
        } catch (IOException e) {
            return Result.error("文件读取失败，请重试！");
        }
        String realExt = FileSecurityUtil.detectImageExt(fileBytes);
        if (realExt == null || !realExt.equals(ext)) {
            return Result.badRequest("文件内容不是合法图片，禁止上传！");
        }

        //6、按“原文件名_年月日时分秒毫秒.ext”生成存储名，根目录 containment 校验后落盘；
        //   同一毫秒同名上传时追加短随机后缀，避免互相覆盖
        String storedName;
        try {
            storedName = FileSecurityUtil.buildTimestampedStoredName(originalFilename, realExt);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
        String dbRelativePath = subPath.isEmpty() ? storedName : subPath + "/" + storedName;
        Path target;
        try {
            target = FileSecurityUtil.resolveUnderRoot(Paths.get(uploadPath), dbRelativePath);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
        if (Files.exists(target)) {
            storedName = FileSecurityUtil.buildTimestampedStoredName(originalFilename, realExt,
                    FileSecurityUtil.shortHexSuffix());
            dbRelativePath = subPath.isEmpty() ? storedName : subPath + "/" + storedName;
            try {
                target = FileSecurityUtil.resolveUnderRoot(Paths.get(uploadPath), dbRelativePath);
            } catch (IllegalArgumentException e) {
                return Result.badRequest(e.getMessage());
            }
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, fileBytes);
        } catch (IOException e) {
            return Result.error("文件保存失败，请重试！");
        }
        return Result.success(dbRelativePath);
    }

    /**
     * 预览图片 / 下载文件
     * 安全约束：相对路径经规范化后必须仍位于上传根目录内（拒绝“..”、绝对路径、编码穿越、符号链接逃逸）；
     * 图片以 inline 方式预览，其余文件以 attachment 下载。
     *
     * @param request
     * @param response
     */
    @GetMapping(value = "/static/**")
    public void view(HttpServletRequest request, HttpServletResponse response) {
        //容器已对 URL 完成一次解码，此处禁止再次 URLDecoder.decode（双重解码会把 %252e%252e 还原成 .. 形成绕过）
        String relPath = extractPathFromPattern(request);
        if (relPath == null || relPath.isBlank()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path root = Paths.get(uploadPath);
        Path target;
        try {
            target = FileSecurityUtil.resolveUnderRoot(root, relPath);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!Files.isRegularFile(target)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        //toRealPath 解析符号链接后再次校验，防止链接逃逸
        Path rootReal;
        Path targetReal;
        try {
            rootReal = root.toRealPath();
            targetReal = target.toRealPath();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!targetReal.startsWith(rootReal)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String fileName = targetReal.getFileName().toString();
        String ext;
        try {
            ext = FileSecurityUtil.extensionOf(fileName);
        } catch (IllegalArgumentException e) {
            ext = "";
        }
        String contentType = IMAGE_CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
        boolean inline = IMAGE_CONTENT_TYPES.containsKey(ext);
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType(contentType);
        response.setHeader("Content-Disposition",
                (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encodedName);
        try {
            response.setContentLengthLong(Files.size(targetReal));
            try (OutputStream outputStream = response.getOutputStream()) {
                Files.copy(targetReal, outputStream);
            }
            response.flushBuffer();
        } catch (IOException e) {
            //客户端中断等写入异常无需处理
        }
    }

    /**
     * 把指定URL后的字符串全部截断当成参数
     * 这么做是为了防止URL中包含中文或者特殊字符（/等）时，匹配不了的问题
     *
     * @param request
     * @return
     */
    private static String extractPathFromPattern(final HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
    }
}
