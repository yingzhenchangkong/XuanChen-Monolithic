package com.xuanchen.filemanage.office;

import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.mapper.FileInfoMapper;
import com.xuanchen.filemanage.service.IFileCollaboratorService;
import com.xuanchen.filemanage.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 服务-->ONLYOFFICE 协同编辑（编辑器配置构建 / 回调保存 / 文档下发）
 * <p>可编辑扩展名：docx/doc/xlsx/xls/pptx/ppt/csv/txt；保存回写直接覆盖原存储对象
 * （file_info.bucket/objectKey 不变），同步刷新 file_size/md5/update_time。</p>
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Service
public class OfficeEditService {

    /** 允许在线编辑的扩展名 → DS documentType 映射（word/cell/slide） */
    private static final Map<String, String> EDITABLE = Map.of(
            "docx", "word", "doc", "word", "txt", "word",
            "xlsx", "cell", "xls", "cell", "csv", "cell",
            "pptx", "slide", "ppt", "slide");

    private static final Set<String> ALL_EDITABLE = EDITABLE.keySet();

    @Autowired
    private OfficeProperties properties;
    @Autowired
    private OfficeJwtUtil jwtUtil;
    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private StorageService storageService;
    @Autowired
    private RestTemplate officeRestTemplate;
    @Autowired
    private IFileCollaboratorService collaboratorService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    public boolean isEditable(String ext) {
        return ext != null && ALL_EDITABLE.contains(ext.toLowerCase());
    }

    /**
     * 构建编辑器配置（前端直接喂给 DocsAPI.DocEditor）
     * <p>校验：文件存在未删、扩展名可编辑、访问权（属主 / admin / 协作者）。</p>
     */
    public Map<String, Object> buildConfig(String fileId, String username, boolean isAdmin) {
        return buildConfig(fileId, username, isAdmin, false);
    }

    /**
     * 构建编辑器配置（重载：viewOnly=true 为只读预览模式，供原格式预览复用）
     * <p>访问权：属主 / admin / 协作者（file_collaborator）；只读协作者（permission=2）强制 view 模式。</p>
     */
    public Map<String, Object> buildConfig(String fileId, String username, boolean isAdmin, boolean viewOnly) {
        FileInfo file = requireVisible(fileId, username, isAdmin);
        int role = roleOf(file, username, isAdmin);
        if (role == ROLE_NONE) {
            throw new IllegalArgumentException("无权访问他人文件");
        }
        //只读协作者（或调用方要求预览）强制 view 模式
        boolean effectiveViewOnly = viewOnly || role == ROLE_COLLAB_READONLY;
        String ext = file.getExt().toLowerCase();
        if (!isEditable(ext)) {
            throw new IllegalArgumentException("该格式暂不支持在线协同编辑：" + file.getExt());
        }
        //文档 key：内容变化（update_time/fileSize）后必然产生新 key，DS 依此判定版本
        String key = DigestUtils.md5DigestAsHex((file.getId() + ":" + file.getUpdateTime() + ":" + file.getFileSize())
                .getBytes(StandardCharsets.UTF_8)).substring(0, 20);
        String docUrl = properties.getPublicBaseUrl() + "/office/download?fileId=" + file.getId()
                + "&st=" + jwtUtil.downloadToken(file.getId(), 1800);

        Map<String, Object> permissions = new HashMap<>();
        permissions.put("edit", !effectiveViewOnly);
        permissions.put("comment", !effectiveViewOnly);
        permissions.put("fillForms", !effectiveViewOnly);
        permissions.put("review", !effectiveViewOnly);
        permissions.put("print", true);
        permissions.put("download", true);

        Map<String, Object> document = new HashMap<>();
        document.put("documentType", EDITABLE.get(ext));
        document.put("fileType", ext);
        document.put("key", key);
        document.put("title", file.getOriginalName());
        document.put("url", docUrl);
        document.put("permissions", permissions);

        //🔴 user.id 必须是当前登录用户（唯一）：DS 以 id 识别协作者身份，
        //若多人共用同一 id，光标/姓名/修订归属全部错乱。展示名优先取昵称
        Map<String, Object> user = new HashMap<>();
        user.put("id", username);
        user.put("name", displayName(username));

        Map<String, Object> customization = new HashMap<>();
        customization.put("forcesave", !effectiveViewOnly);
        customization.put("compactHeader", true);
        customization.put("hideRightMenus", false);
        customization.put("uiTheme", "theme-light");

        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("callbackUrl", properties.getPublicBaseUrl() + "/office/callback?xcfileid=" + file.getId());
        editorConfig.put("lang", "zh-CN");
        editorConfig.put("mode", effectiveViewOnly ? "view" : "edit");
        editorConfig.put("user", user);
        editorConfig.put("customization", customization);

        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("documentType", EDITABLE.get(ext));
        config.put("editorConfig", editorConfig);
        config.put("width", "100%");
        config.put("height", "100%");
        config.put("token", jwtUtil.sign(config));

        Map<String, Object> result = new HashMap<>();
        result.put("dsUrl", properties.getDsUrl());
        result.put("config", config);
        return result;
    }

    /**
     * DS 保存回调：status 2/6（保存/关闭带修改）时拉回文档覆盖原对象
     * <p>整个回调体由 DS 用同一 JWT 密钥签名，先验签再落库。</p>
     */
    @SuppressWarnings("unchecked")
    public void handleCallback(String fileId, Map<String, Object> body) {
        Object token = body.get("token");
        if (token == null) {
            throw new IllegalArgumentException("回调缺少 JWT（请为 Document Server 配置相同的 JWT_SECRET）");
        }
        jwtUtil.verify(String.valueOf(token));

        int status = body.get("status") instanceof Number n ? n.intValue() : 0;
        if (status != 2 && status != 6) {
            return; //1=继续编辑无修改 4=关闭无修改 7=强制保存出错，均无需处理
        }
        String downloadUrl = String.valueOf(body.get("url"));
        if (downloadUrl == null || downloadUrl.isBlank() || "null".equals(downloadUrl)) {
            throw new IllegalArgumentException("回调缺少下载地址 url");
        }
        //fileId 由 callbackUrl 的 query 参数传入（DS 原样回传 callbackUrl 含 query）
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("回调文件不存在：" + fileId);
        }
        byte[] bytes = officeRestTemplate.getForObject(downloadUrl, byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("回调文档内容为空");
        }
        String md5 = DigestUtils.md5DigestAsHex(bytes);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            storageService.put(file.getBucket(), file.getObjectKey(), in, bytes.length, file.getContentType());
        } catch (Exception e) {
            throw new IllegalStateException("保存文档到存储失败：" + e.getMessage(), e);
        }
        FileInfo update = new FileInfo();
        update.setId(file.getId());
        update.setFileSize((long) bytes.length);
        update.setMd5(md5);
        update.setUpdateTime(LocalDateTime.now());
        fileInfoMapper.updateById(update);
        log.info("ONLYOFFICE 协同保存完成 fileId={} size={}", file.getId(), bytes.length);
    }

    /** 下载文档给 DS（已由 Controller 校验签名令牌） */
    public ResponseEntity<byte[]> loadDocument(String fileId) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes;
        try (InputStream in = storageService.get(file.getBucket(), file.getObjectKey())) {
            bytes = in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("读取文档失败：" + e.getMessage(), e);
        }
        return ResponseEntity.ok()
                .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .header("Content-Disposition", "attachment; filename*=UTF-8''"
                        + java.net.URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20"))
                .body(bytes);
    }

    /** 无权访问 */
    private static final int ROLE_NONE = 0;
    /** 只读协作者 */
    private static final int ROLE_COLLAB_READONLY = 2;

    /**
     * 计算当前用户对文件的协作身份：-1=属主/admin，1=可编辑协作者，2=只读协作者，0=无权
     */
    private int roleOf(FileInfo file, String username, boolean isAdmin) {
        if (isAdmin || username.equals(file.getUsername())) {
            return -1;
        }
        Integer permission = collaboratorService.permissionOf(file.getId(), username);
        return permission == null ? ROLE_NONE : permission;
    }

    /**
     * 展示名：优先取账号昵称（sys_account.nick_name），取不到回落登录名
     */
    private String displayName(String username) {
        try {
            Map<String, Object> account = authServiceCommon.getUserByUserName(username);
            Object nick = account == null ? null : account.get("nickName");
            if (nick != null && !String.valueOf(nick).isBlank()) {
                return String.valueOf(nick);
            }
        } catch (Exception e) {
            log.warn("读取用户昵称失败 username={}", username, e);
        }
        return username;
    }

    /** 加载并校验文件存在未删（访问权由 roleOf 判定） */
    private FileInfo requireVisible(String fileId, String username, boolean isAdmin) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在或已删除");
        }
        return file;
    }
}
