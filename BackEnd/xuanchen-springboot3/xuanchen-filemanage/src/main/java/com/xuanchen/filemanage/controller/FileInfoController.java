package com.xuanchen.filemanage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.service.IFileInfoService;
import com.xuanchen.filemanage.service.IFileQuotaService;
import com.xuanchen.filemanage.service.IFileShareService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 控制器-->文件中心通用文件
 * 安全约束：扩展名白名单 + 魔数校验 + 大小上限 + 归属校验（详见 FileInfoServiceImpl / FileMagicUtil）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@RestController
@RequestMapping("/fileinfo")
public class FileInfoController {

    /**
     * 文本预览单次最多读取的字节数：几十 MB 的日志整篇塞进 JSON 会把浏览器卡死，超出部分截断
     */
    private static final int TEXT_PREVIEW_MAX_BYTES = 512 * 1024;

    @Autowired
    private IFileInfoService fileInfoService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;
    @Autowired
    private IFileQuotaService fileQuotaService;
    @Autowired
    private IFileShareService fileShareService;
    @Autowired
    private com.xuanchen.filemanage.service.IFileFolderService fileFolderService;
    @Autowired
    private com.xuanchen.filemanage.service.IFileChunkService fileChunkService;

    /**
     * 通用文件上传
     *
     * @param request    multipart 请求（file=文件，bizType=业务类型，默认 common）
     * @return 元数据摘要 {id, name, ext, size, previewUrl}
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        MultipartFile file = null;
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            file = multipartRequest.getFile("file");
        }
        String bizType = request.getParameter("bizType");
        String folderId = request.getParameter("folderId");
        try {
            FileInfo info = fileInfoService.upload(file, bizType == null || bizType.isBlank() ? "common" : bizType,
                    username, folderId);
            Map<String, Object> data = new HashMap<>();
            data.put("id", info.getId());
            data.put("name", info.getOriginalName());
            data.put("ext", info.getExt());
            data.put("size", info.getFileSize());
            data.put("previewUrl", fileInfoService.getUrl(info.getId(), isInline(info.getExt()),
                    username, isAdmin()));
            return Result.success("上传成功!", data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败，请重试！");
        }
    }

    /**
     * 我的文件分页
     */
    @GetMapping("/my")
    public Result<Page<FileInfo>> my(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                     @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                     @RequestParam(name = "keyword", required = false) String keyword,
                                     @RequestParam(name = "ext", required = false) String ext,
                                     @RequestParam(name = "folderId", required = false) String folderId,
                                     HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(fileInfoService.pageMy(pageNo, pageSize, keyword, ext, folderId, username));
    }

    /**
     * 获取预览/下载地址（预签名 URL 有有效期，禁止落库缓存）
     */
    @GetMapping("/url")
    public Result<String> url(@RequestParam(name = "id") String id,
                              @RequestParam(name = "inline", defaultValue = "false") boolean inline,
                              HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            //注意：success(T) 与 success(String msg) 重载歧义，String 数据必须走双参重载进 data
            return Result.success("获取成功!", fileInfoService.getUrl(id, inline, username, isAdmin()));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取文件地址失败 id={}", id, e);
            return Result.error("获取文件地址失败，请重试！");
        }
    }

    /**
     * 文本类文件内容（文本在线预览用）
     * 不直接把预签名 URL 交给 iframe：text/plain 不带 charset 时浏览器按默认编码渲染，中文必乱码；
     * 这里由服务端读出字节、按 BOM/UTF-8/GB18030 探测编码后以 JSON 返回，前端用 pre 渲染。
     *
     * @param id      文件 id
     * @param charset 指定编码（空=自动探测），用于前端手动切换编码
     * @return {content, charset, truncated, fileSize}
     */
    @GetMapping("/text")
    public Result<Map<String, Object>> text(@RequestParam(name = "id") String id,
                                           @RequestParam(name = "charset", required = false) String charset,
                                           HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success("获取成功!", fileInfoService.textContent(id, charset, TEXT_PREVIEW_MAX_BYTES,
                    username, isAdmin()));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文本内容预览失败 id={}", id, e);
            return Result.error("文本预览失败，请重试！");
        }
    }

    /**
     * 删除我的文件（进回收站：逻辑删+记录删除时间，存储对象保留）
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            fileInfoService.delete(id, username, isAdmin());
            return Result.success("已移入回收站!");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文件删除失败 id={}", id, e);
            return Result.error("文件删除失败，请重试！");
        }
    }

    /**
     * 回收站分页（admin 可见全部）
     */
    @GetMapping("/recycle")
    public Result<Page<FileInfo>> recycle(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                          @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                          @RequestParam(name = "keyword", required = false) String keyword,
                                          HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(fileInfoService.pageRecycle(pageNo, pageSize, keyword, isAdmin() ? null : username));
    }

    /**
     * 从回收站恢复
     */
    @PostMapping("/restore")
    public Result<String> restore(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            fileInfoService.restore(id, username, isAdmin());
            return Result.success("恢复成功!");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文件恢复失败 id={}", id, e);
            return Result.error("文件恢复失败，请重试！");
        }
    }

    /**
     * 彻底删除（物理删元数据；存储对象无其他引用时一并清理）
     */
    @DeleteMapping("/purge")
    public Result<String> purge(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            fileInfoService.purge(id, username, isAdmin());
            return Result.success("已彻底删除!");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文件彻底删除失败 id={}", id, e);
            return Result.error("彻底删除失败，请重试！");
        }
    }

    /**
     * 秒传探测：凭文件名+MD5 尝试免传建元数据；data=null 表示未命中（转常规上传）
     */
    @GetMapping("/instant")
    public Result<Map<String, Object>> instant(@RequestParam(name = "name") String name,
                                               @RequestParam(name = "md5") String md5,
                                               @RequestParam(name = "bizType", defaultValue = "common") String bizType,
                                               @RequestParam(name = "folderId", required = false) String folderId,
                                               HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            FileInfo info = fileInfoService.instantUpload(name, md5, bizType, username, folderId);
            if (info == null) {
                return Result.success(null);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("id", info.getId());
            data.put("name", info.getOriginalName());
            data.put("ext", info.getExt());
            data.put("size", info.getFileSize());
            data.put("previewUrl", fileInfoService.getUrl(info.getId(), isInline(info.getExt()),
                    username, isAdmin()));
            return Result.success("秒传成功!", data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("秒传探测失败 name={}", name, e);
            return Result.error("秒传探测失败，请重试！");
        }
    }

    /**
     * 我的配额用量
     */
    @GetMapping("/quota/my")
    public Result<Map<String, Object>> quotaMy(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(fileQuotaService.myUsage(username));
    }

    /**
     * 配额用量总览（仅 admin）
     */
    @GetMapping("/quota/list")
    public Result<List<Map<String, Object>>> quotaList(HttpServletRequest request) {
        if (!isAdmin()) {
            return Result.badRequest("仅管理员可查看配额总览！");
        }
        return Result.success(fileQuotaService.listQuotaUsage());
    }

    /**
     * 保存用户配额（仅 admin）：quotaBytes=-1 表示恢复系统默认
     */
    @PostMapping("/quota/save")
    public Result<String> quotaSave(@RequestParam(name = "username") String username,
                                    @RequestParam(name = "quotaBytes") long quotaBytes,
                                    HttpServletRequest request) {
        if (!isAdmin()) {
            return Result.badRequest("仅管理员可设置配额！");
        }
        String operator = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            fileQuotaService.saveQuota(username, quotaBytes, operator);
            return Result.success("配额已保存!");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("配额保存失败 username={}", username, e);
            return Result.error("配额保存失败，请重试！");
        }
    }

    /**
     * 创建共享（仅文件归属人）：返回 shareNo，前端拼公开链接
     */
    @PostMapping("/share/save")
    public Result<Map<String, Object>> shareSave(@RequestParam(name = "fileId") String fileId,
                                                 @RequestParam(name = "expireHours") Integer expireHours,
                                                 @RequestParam(name = "shareCode", required = false) String shareCode,
                                                 HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            com.xuanchen.filemanage.entity.FileShare share = fileShareService.create(fileId, expireHours,
                    shareCode, username);
            Map<String, Object> data = new HashMap<>();
            data.put("id", share.getId());
            data.put("shareNo", share.getShareNo());
            data.put("expireTime", share.getExpireTime());
            return Result.success("共享创建成功!", data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("共享创建失败 fileId={}", fileId, e);
            return Result.error("共享创建失败，请重试！");
        }
    }

    /**
     * 我的共享列表（含文件名冗余，按创建时间倒序）
     */
    @GetMapping("/share/my")
    public Result<List<Map<String, Object>>> shareMy(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        List<com.xuanchen.filemanage.entity.FileShare> shares = fileShareService.listMine(username);
        List<Map<String, Object>> rows = shares.stream().map(share -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", share.getId());
            row.put("shareNo", share.getShareNo());
            row.put("fileId", share.getFileId());
            row.put("expireTime", share.getExpireTime());
            row.put("visitCount", share.getVisitCount());
            row.put("hasCode", share.getShareCode() != null && !share.getShareCode().isBlank());
            row.put("createTime", share.getCreateTime());
            FileInfo info = fileInfoService.getById(share.getFileId());
            row.put("fileName", info != null ? info.getOriginalName() : "（文件已删除）");
            row.put("fileExt", info != null ? info.getExt() : "");
            row.put("fileSize", info != null ? info.getFileSize() : 0);
            return row;
        }).toList();
        return Result.success(rows);
    }

    /**
     * 撤销共享（本人或 admin）
     */
    @DeleteMapping("/share/delete")
    public Result<String> shareDelete(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            fileShareService.revoke(id, username, isAdmin());
            return Result.success("已撤销!");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("共享撤销失败 id={}", id, e);
            return Result.error("共享撤销失败，请重试！");
        }
    }

    // ==================== 文件夹（三期） ====================

    /**
     * 我的文件夹列表（平铺，前端组树）
     */
    @GetMapping("/folder/tree")
    public Result<List<com.xuanchen.filemanage.entity.FileFolder>> folderTree(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(fileFolderService.listMine(username));
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folder/create")
    public Result<com.xuanchen.filemanage.entity.FileFolder> folderCreate(
            @RequestParam(name = "parentId", defaultValue = "0") String parentId,
            @RequestParam(name = "folderName") String folderName,
            HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success("创建成功!", fileFolderService.create(parentId, folderName, username));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文件夹创建失败", e);
            return Result.error("文件夹创建失败，请重试！");
        }
    }

    /**
     * 重命名文件夹
     */
    @PostMapping("/folder/rename")
    public Result<String> folderRename(@RequestParam(name = "id") String id,
                                       @RequestParam(name = "folderName") String folderName,
                                       HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            fileFolderService.rename(id, folderName, username);
            return Result.success("重命名成功!");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文件夹重命名失败 id={}", id, e);
            return Result.error("文件夹重命名失败，请重试！");
        }
    }

    /**
     * 删除文件夹（递归子树；其下文件整体移入回收站）
     */
    @PostMapping("/folder/delete")
    public Result<String> folderDelete(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            fileFolderService.delete(id, username);
            return Result.success("删除成功! 文件夹及其下文件已删除（文件可在回收站恢复）");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文件夹删除失败 id={}", id, e);
            return Result.error("文件夹删除失败，请重试！");
        }
    }

    // ==================== 分片上传（三期） ====================

    /**
     * 分片任务初始化：秒传命中返回 {instant:true, file}；
     * 否则返回 {uploadId, uploaded}（uploaded=已传分片索引，断点续传续传剩余分片）
     */
    @PostMapping("/chunk/init")
    public Result<Map<String, Object>> chunkInit(@RequestParam(name = "fileName") String fileName,
                                                 @RequestParam(name = "fileSize") long fileSize,
                                                 @RequestParam(name = "md5") String md5,
                                                 @RequestParam(name = "bizType", defaultValue = "common") String bizType,
                                                 @RequestParam(name = "folderId", required = false) String folderId,
                                                 @RequestParam(name = "totalChunks") int totalChunks,
                                                 @RequestParam(name = "chunkSize") long chunkSize,
                                                 HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success(fileChunkService.init(fileName, fileSize, md5, bizType, folderId,
                    totalChunks, chunkSize, username));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("分片任务初始化失败 fileName={}", fileName, e);
            return Result.error("分片任务初始化失败，请重试！");
        }
    }

    /**
     * 保存单片（幂等：重复上传同一片按已处理）
     */
    @PostMapping("/chunk")
    public Result<String> chunk(@RequestParam(name = "uploadId") String uploadId,
                                @RequestParam(name = "index") int index,
                                HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        MultipartFile file = null;
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            file = multipartRequest.getFile("file");
        }
        try {
            fileChunkService.saveChunk(uploadId, index, file, username);
            return Result.success("分片已保存!");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("分片保存失败 uploadId={} index={}", uploadId, index, e);
            return Result.error("分片保存失败，请重试！");
        }
    }

    /**
     * 完成合并：返回 {file}（与秒传/常规上传摘要同构）
     */
    @PostMapping("/chunk/complete")
    public Result<Map<String, Object>> chunkComplete(@RequestParam(name = "uploadId") String uploadId,
                                                     HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success("上传成功!", fileChunkService.complete(uploadId, username));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("分片合并失败 uploadId={}", uploadId, e);
            return Result.error("分片合并失败，请重试！");
        }
    }

    private static boolean isInline(String ext) {
        return ext != null && Set.of("jpg", "png", "gif", "webp", "bmp", "pdf", "txt", "md", "csv", "json")
                .contains(ext);
    }

    private static boolean isAdmin() {
        //sb3 鉴权走 Shiro：与 sb4 hasRole('admin') 同构
        return SecurityUtils.getSubject().hasRole("admin");
    }
}
