package com.xuanchen.filemanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.FileSecurityUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.mapper.FileInfoMapper;
import com.xuanchen.filemanage.service.IFileInfoService;
import com.xuanchen.filemanage.service.IFileQuotaService;
import com.xuanchen.filemanage.storage.StorageService;
import com.xuanchen.filemanage.storage.StorageProperties;
import com.xuanchen.filemanage.util.FileMagicUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Service接口实现类-->文件元数据（文件中心）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileInfoService {

    /**
     * 业务类型白名单：同时作为 objectKey 首段，必须路径安全
     */
    private static final Pattern BIZ_TYPE_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,31}$");

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 文本类扩展名：允许走 /fileinfo/text 内容预览（其余类型只签 URL）
     */
    private static final Set<String> TEXT_EXTS = Set.of("txt", "md", "markdown", "csv", "json",
            "log", "xml", "yml", "yaml", "ini", "properties", "sql");

    /**
     * 扩展名 -> Content-Type 兜底映射
     */
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("json", "application/json"),
            Map.entry("zip", "application/zip"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("7z", "application/x-7z-compressed"));

    /**
     * MD5 合法格式（32 位十六进制）
     */
    private static final Pattern MD5_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final IFileQuotaService fileQuotaService;
    private final com.xuanchen.filemanage.service.IFileFolderService fileFolderService;

    @Value("${xuanchen.fileinfo.max-size:50MB}")
    private String maxSize;

    public FileInfoServiceImpl(StorageService storageService, StorageProperties storageProperties,
                               IFileQuotaService fileQuotaService,
                               @org.springframework.context.annotation.Lazy
                               com.xuanchen.filemanage.service.IFileFolderService fileFolderService) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.fileQuotaService = fileQuotaService;
        this.fileFolderService = fileFolderService;
    }

    @Override
    public FileInfo upload(MultipartFile file, String bizType, String username) {
        return upload(file, bizType, username, null);
    }

    @Override
    public FileInfo upload(MultipartFile file, String bizType, String username, String folderId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空！");
        }
        String originalFilename;
        byte[] fileBytes;
        try {
            originalFilename = FileSecurityUtil.stripClientPath(file.getOriginalFilename());
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("文件读取失败，请重试！");
        }
        return store(fileBytes, originalFilename, bizType, username, folderId);
    }

    @Override
    public FileInfo store(byte[] bytes, String originalFilename, String bizType, String username) {
        return store(bytes, originalFilename, bizType, username, null);
    }

    @Override
    public FileInfo store(byte[] bytes, String originalFilename, String bizType, String username, String folderId) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空！");
        }
        if (bizType == null || bizType.isBlank() || !BIZ_TYPE_PATTERN.matcher(bizType).matches()) {
            throw new IllegalArgumentException("业务类型非法（仅允许小写字母开头的字母/数字/下划线/中划线，最长32）！");
        }
        //0、文件夹归属校验（空=根目录）
        String targetFolderId = null;
        if (folderId != null && !folderId.isBlank() && !"0".equals(folderId)) {
            if (!fileFolderService.existsValid(folderId, username)) {
                throw new IllegalArgumentException("目标文件夹不存在或无权操作！");
            }
            targetFolderId = folderId;
        }
        //1、大小校验
        long maxBytes = DataSize.parse(maxSize).toBytes();
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("文件大小超出限制（最大 " + maxSize + "）！");
        }
        //2、扩展名白名单 + 魔数真实类型校验
        String ext;
        try {
            ext = FileMagicUtil.validateGeneral(originalFilename, bytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        //3、桶选择：头像等公开资源走公开桶，其余走默认附件桶
        String bucket = ("avatar".equals(bizType) || "photo".equals(bizType))
                ? storageProperties.getPublicBucket()
                : storageProperties.getDefaultBucket();
        String md5 = md5Hex(bytes);
        //4、秒传/存储去重：同用户+同MD5+同桶已有有效记录时复用其存储对象，仅新增元数据
        FileInfo dup = null;
        if (md5 != null) {
            dup = this.lambdaQuery().eq(FileInfo::getUsername, username)
                    .eq(FileInfo::getMd5, md5).eq(FileInfo::getBucket, bucket)
                    .last("LIMIT 1").one();
        }
        if (dup != null) {
            FileInfo info = buildDedupRecord(dup, originalFilename, bizType, username, md5);
            info.setFolderId(targetFolderId);
            this.save(info);
            return info;
        }
        //5、配额校验（按用户逻辑文件总大小计，超出直接拒绝；0=不限制）
        long quotaBytes = fileQuotaService.getEffectiveQuotaBytes(username);
        if (quotaBytes > 0) {
            long used = baseMapper.sumUsedBytes(username);
            if (used + bytes.length > quotaBytes) {
                throw new IllegalArgumentException(String.format(
                        "存储配额不足：已用 %s，配额上限 %s，本次需 %s，请清理文件或联系管理员调整配额！",
                        humanSize(used), humanSize(quotaBytes), humanSize(bytes.length)));
            }
        }
        //6、服务端生成存储文件名与 objectKey（含时间戳防重，主名安全性由 buildTimestampedStoredName 保证）
        String storedName = FileSecurityUtil.buildTimestampedStoredName(originalFilename, ext,
                FileSecurityUtil.shortHexSuffix());
        String objectKey = bizType + "/" + LocalDateTime.now().format(MONTH_FORMATTER) + "/" + storedName;
        //7、落存储（MinIO 不可用会直接抛异常，不静默降级）
        String contentType = CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
        storageService.put(bucket, objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType);
        //8、元数据入库
        FileInfo info = new FileInfo();
        info.setBucket(bucket);
        info.setObjectKey(objectKey);
        info.setOriginalName(originalFilename);
        info.setExt(ext);
        info.setContentType(contentType);
        info.setFileSize((long) bytes.length);
        info.setStorageType("minio".equalsIgnoreCase(storageProperties.getType()) ? 1 : 0);
        info.setBizType(bizType);
        info.setUsername(username);
        info.setMd5(md5);
        info.setFolderId(targetFolderId);
        info.setCreateTime(LocalDateTime.now());
        this.save(info);
        return info;
    }

    @Override
    public Page<FileInfo> pageMy(long pageNo, long pageSize, String keyword, String ext, String folderId,
                                 String username) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        if ("0".equals(folderId)) {
            //仅根目录
            queryWrapper.and(q -> q.isNull("folder_id").or().eq("folder_id", ""));
        } else if (StringUtil.isNotEmpty(folderId)) {
            queryWrapper.eq("folder_id", folderId);
        }
        if (StringUtil.isNotEmpty(keyword)) {
            queryWrapper.like("original_name", keyword);
        }
        if (StringUtil.isNotEmpty(ext)) {
            queryWrapper.eq("ext", ext.toLowerCase());
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public String getUrl(String id, boolean inline, String username, boolean isAdmin) {
        FileInfo info = this.getById(id);
        if (info == null) {
            throw new IllegalArgumentException("文件不存在或已被删除！");
        }
        checkOwner(info, username, isAdmin);
        return storageService.previewUrl(info.getBucket(), info.getObjectKey(), info.getOriginalName(), inline,
                info.getContentType());
    }

    @Override
    public Map<String, Object> textContent(String id, String charset, int maxBytes, String username, boolean isAdmin) {
        FileInfo info = this.getById(id);
        if (info == null) {
            throw new IllegalArgumentException("文件不存在或已被删除！");
        }
        checkOwner(info, username, isAdmin);
        String ext = info.getExt() == null ? "" : info.getExt().toLowerCase(Locale.ROOT);
        if (!TEXT_EXTS.contains(ext)) {
            throw new IllegalArgumentException("该文件不是文本类型，不支持文本预览！");
        }
        byte[] bytes;
        boolean truncated;
        try (InputStream in = storageService.get(info.getBucket(), info.getObjectKey())) {
            //多读 1 字节用于判断是否截断，避免把整个大文件读进内存
            byte[] buffer = in.readNBytes(Math.max(1, maxBytes) + 1);
            truncated = buffer.length > maxBytes;
            bytes = truncated ? Arrays.copyOf(buffer, maxBytes) : buffer;
        } catch (Exception e) {
            log.error("文本内容读取失败 id={} objectKey={}", id, info.getObjectKey(), e);
            throw new IllegalStateException("文件内容读取失败，请重试！");
        }
        Charset target;
        if (StringUtil.isEmpty(charset)) {
            target = detectCharset(bytes);
        } else {
            target = charsetOf(charset);
            if (target == null) {
                throw new IllegalArgumentException("不支持的文本编码：" + charset);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        //截断可能切断多字节字符，解码时按字符边界收尾，不产生半个汉字
        data.put("content", decodeText(bytes, target));
        data.put("charset", target.name());
        data.put("truncated", truncated);
        data.put("fileSize", info.getFileSize());
        return data;
    }

    @Override
    public void delete(String id, String username, boolean isAdmin) {
        FileInfo info = this.getById(id);
        if (info == null) {
            throw new IllegalArgumentException("文件不存在或已被删除！");
        }
        checkOwner(info, username, isAdmin);
        //二期：删除=进回收站（逻辑删+记录删除时间），存储对象保留待恢复/超期清理
        this.lambdaUpdate().eq(FileInfo::getId, id)
                .set(FileInfo::getDelFlag, 1)
                .set(FileInfo::getDeleteTime, LocalDateTime.now())
                .update();
    }

    @Override
    public Page<FileInfo> pageRecycle(long pageNo, long pageSize, String keyword, String username) {
        //admin 传 username=null 查看全部回收站；MP 自定义分页会原样返回传入的 Page 实例
        @SuppressWarnings("unchecked")
        Page<FileInfo> page = (Page<FileInfo>) baseMapper.pageRecycle(new Page<>(pageNo, pageSize),
                StringUtil.isEmpty(username) ? null : username, keyword);
        return page;
    }

    @Override
    public void restore(String id, String username, boolean isAdmin) {
        FileInfo info = baseMapper.selectByIdIgnoreDeleted(id);
        if (info == null) {
            throw new IllegalArgumentException("文件不存在！");
        }
        checkOwner(info, username, isAdmin);
        //原文件夹已被删除时回落到根目录（null）
        String targetFolderId = (info.getFolderId() != null
                && fileFolderService.existsValid(info.getFolderId(), info.getUsername()))
                ? info.getFolderId() : null;
        //必须走自定义 SQL：lambdaUpdate 受 @TableLogic 影响更新不到已删行
        baseMapper.restoreById(id, targetFolderId);
    }

    @Override
    public void purge(String id, String username, boolean isAdmin) {
        FileInfo info = baseMapper.selectByIdIgnoreDeleted(id);
        if (info == null) {
            throw new IllegalArgumentException("文件不存在！");
        }
        checkOwner(info, username, isAdmin);
        baseMapper.deleteByIdPhysical(id);
        removeStorageIfOrphan(info);
    }

    @Override
    public FileInfo instantUpload(String originalFilename, String md5, String bizType, String username,
                                  String folderId) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空！");
        }
        if (md5 == null || !MD5_PATTERN.matcher(md5).matches()) {
            throw new IllegalArgumentException("MD5 格式非法！");
        }
        if (bizType == null || bizType.isBlank() || !BIZ_TYPE_PATTERN.matcher(bizType).matches()) {
            throw new IllegalArgumentException("业务类型非法！");
        }
        String ext = FileSecurityUtil.extensionOf(originalFilename);
        if (!FileMagicUtil.GENERAL_ALLOW.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型：" + ext);
        }
        String bucket = ("avatar".equals(bizType) || "photo".equals(bizType))
                ? storageProperties.getPublicBucket()
                : storageProperties.getDefaultBucket();
        //命中条件：同一用户、同 MD5、同目标桶存在有效记录（防跨用户探测文件存在性）
        FileInfo dup = this.lambdaQuery().eq(FileInfo::getUsername, username)
                .eq(FileInfo::getMd5, md5.toLowerCase()).eq(FileInfo::getBucket, bucket)
                .last("LIMIT 1").one();
        if (dup == null) {
            return null;
        }
        FileInfo info = buildDedupRecord(dup, originalFilename, dup.getExt(), username, md5.toLowerCase());
        String targetFolderId = (folderId != null && !folderId.isBlank() && !"0".equals(folderId)
                && fileFolderService.existsValid(folderId, username)) ? folderId : null;
        info.setFolderId(targetFolderId);
        this.save(info);
        return info;
    }

    @Override
    public int cleanExpiredRecycle(int keepDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(keepDays);
        List<FileInfo> expired = baseMapper.listRecycleExpired(deadline);
        int cleaned = 0;
        for (FileInfo info : expired) {
            try {
                baseMapper.deleteByIdPhysical(info.getId());
                removeStorageIfOrphan(info);
                cleaned++;
            } catch (Exception e) {
                log.warn("回收站清理失败 id={} name={}", info.getId(), info.getOriginalName(), e);
            }
        }
        return cleaned;
    }

    /**
     * 存储对象孤儿清理：引用计数为 0 且为 MinIO 对象时异步删除（本地存储不删，留人工兜底）
     */
    private void removeStorageIfOrphan(FileInfo info) {
        long refs = baseMapper.countReferrer(info.getBucket(), info.getObjectKey(), info.getId());
        if (refs == 0 && Integer.valueOf(1).equals(info.getStorageType())) {
            String bucket = info.getBucket();
            String objectKey = info.getObjectKey();
            CompletableFuture.runAsync(() -> {
                try {
                    storageService.remove(bucket, objectKey);
                } catch (Exception e) {
                    log.warn("存储对象清理失败 bucket={} objectKey={}", bucket, objectKey, e);
                }
            });
        }
    }

    private void checkOwner(FileInfo info, String username, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (info.getUsername() == null || !info.getUsername().equals(username)) {
            throw new IllegalArgumentException("无权访问他人文件！");
        }
    }

    /**
     * 文本编码探测：BOM 优先；其次严格校验 UTF-8（含 ASCII）；失败回落 GB18030（GBK/GB2312 超集）。
     * 中文 Windows 记事本存 txt 默认是 GBK，只按 UTF-8 解码必然乱码，故必须探测。
     */
    private static Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (isDecodable(bytes, StandardCharsets.UTF_8)) {
            return StandardCharsets.UTF_8;
        }
        Charset gb18030 = charsetOf("GB18030");
        if (gb18030 != null && isDecodable(bytes, gb18030)) {
            return gb18030;
        }
        return StandardCharsets.UTF_8;
    }

    private static Charset charsetOf(String name) {
        try {
            return Charset.isSupported(name) ? Charset.forName(name) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 严格解码校验：末尾因截断产生的半个字符按「输入未结束」处理，不算非法字节
     */
    private static boolean isDecodable(byte[] bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        //三参 decode 不抛异常，错误以 CoderResult 返回（注意不是便利方法 decode(ByteBuffer)）
        CoderResult result = decoder.decode(ByteBuffer.wrap(bytes), CharBuffer.allocate(bytes.length + 16), false);
        return !result.isError();
    }

    /**
     * 容错解码：非法字节替换为 � 而不是整体失败；去掉 UTF-8 BOM，否则预览首行会多个不可见字符
     */
    private static String decodeText(byte[] bytes, Charset charset) {
        try {
            CharBuffer out = CharBuffer.allocate(bytes.length + 16);
            CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            CoderResult result = decoder.decode(ByteBuffer.wrap(bytes), out, false);
            if (result.isError()) {
                return new String(bytes, charset);
            }
            out.flip();
            String text = out.toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (Exception e) {
            return new String(bytes, charset);
        }
    }

    private String md5Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            //MD5 仅用于秒传去重，失败不影响上传主流程
            return null;
        }
    }

    /**
     * 字节数可读化（配额提示用）
     */
    private static String humanSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        if (size < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", size / 1024.0 / 1024);
        }
        return String.format("%.2f GB", size / 1024.0 / 1024 / 1024);
    }

    /**
     * 秒传/去重复用记录：指向同一存储对象，仅文件名/业务类型/归属人为新值
     */
    private FileInfo buildDedupRecord(FileInfo source, String originalFilename, String bizType,
                                      String username, String md5) {
        FileInfo info = new FileInfo();
        info.setBucket(source.getBucket());
        info.setObjectKey(source.getObjectKey());
        info.setOriginalName(originalFilename);
        info.setExt(source.getExt());
        info.setContentType(source.getContentType());
        info.setFileSize(source.getFileSize());
        info.setStorageType(source.getStorageType());
        info.setBizType(bizType);
        info.setUsername(username);
        info.setMd5(md5);
        info.setCreateTime(LocalDateTime.now());
        return info;
    }
}
