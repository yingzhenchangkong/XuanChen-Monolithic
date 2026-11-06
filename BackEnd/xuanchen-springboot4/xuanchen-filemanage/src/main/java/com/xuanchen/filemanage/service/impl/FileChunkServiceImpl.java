package com.xuanchen.filemanage.service.impl;

import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.service.IFileChunkService;
import com.xuanchen.filemanage.service.IFileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service接口实现类-->分片上传/断点续传
 * 进度元数据：Redis key "filechunk:{uploadId}"（JSON，TTL 24h）；
 * 分片文件：{java.io.tmpdir}/xc-chunks/{uploadId}/{index}；
 * complete 后统一走 IFileInfoService.store（白名单+魔数+配额+秒传+对象去重全套复用）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Service
public class FileChunkServiceImpl implements IFileChunkService {

    /**
     * Redis 键前缀（与项目其他键前缀互不包含）
     */
    public static final String KEY_PREFIX = "filechunk:";

    /**
     * 分片临时根目录
     */
    private static final Path CHUNK_DIR = Path.of(System.getProperty("java.io.tmpdir"), "xc-chunks");

    /**
     * 任务有效期：超期未完成即清理（断点续传窗口）
     */
    private static final Duration TASK_TTL = Duration.ofHours(24);

    /**
     * 分片数上限（50MB/1MB ≈ 50，留余量防滥用）
     */
    private static final int MAX_TOTAL_CHUNKS = 10000;

    /**
     * 单片大小上限
     */
    private static final long MAX_CHUNK_SIZE = 10L * 1024 * 1024;

    private static final Pattern MD5_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IFileInfoService fileInfoService;

    @Override
    public Map<String, Object> init(String fileName, long fileSize, String md5, String bizType,
                                    String folderId, int totalChunks, long chunkSize, String username) {
        //1、参数校验
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空！");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件大小非法！");
        }
        if (md5 == null || !MD5_PATTERN.matcher(md5).matches()) {
            throw new IllegalArgumentException("MD5 格式非法！");
        }
        if (totalChunks < 1 || totalChunks > MAX_TOTAL_CHUNKS) {
            throw new IllegalArgumentException("分片数量非法！");
        }
        if (chunkSize <= 0 || chunkSize > MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("单片大小非法（最大 10MB）！");
        }
        //末片之外不允许单片不满
        if ((totalChunks - 1) * chunkSize >= fileSize || totalChunks * chunkSize < fileSize) {
            throw new IllegalArgumentException("分片参数与文件大小不一致！");
        }
        //2、秒传探测（与常规上传同一套秒传语义）
        try {
            FileInfo info = fileInfoService.instantUpload(fileName, md5, bizType, username, folderId);
            if (info != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("instant", true);
                result.put("file", buildSummary(info));
                return result;
            }
        } catch (IllegalArgumentException ignored) {
            //秒传入口对扩展名白名单校验更严（无法看内容），未命中转分片上传
        }
        //3、断点续传：同用户+同文件名+同大小的未过期任务直接续传
        Map<String, Object> existed = findResumableTask(username, fileName, fileSize);
        if (existed != null) {
            return existed;
        }
        //4、新建任务
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> meta = new HashMap<>();
        meta.put("uploadId", uploadId);
        meta.put("username", username);
        meta.put("fileName", fileName);
        meta.put("fileSize", fileSize);
        meta.put("md5", md5.toLowerCase());
        meta.put("bizType", bizType);
        meta.put("folderId", folderId);
        meta.put("totalChunks", totalChunks);
        meta.put("chunkSize", chunkSize);
        meta.put("createTime", LocalDateTime.now().toString());
        meta.put("uploaded", new ArrayList<Integer>());
        try {
            Files.createDirectories(CHUNK_DIR.resolve(uploadId));
        } catch (Exception e) {
            throw new RuntimeException("分片目录创建失败", e);
        }
        saveMeta(meta);
        Map<String, Object> result = new HashMap<>();
        result.put("instant", false);
        result.put("uploadId", uploadId);
        result.put("uploaded", new ArrayList<Integer>());
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveChunk(String uploadId, int index, MultipartFile file, String username) {
        Map<String, Object> meta = loadMeta(uploadId, username);
        int total = ((Number) meta.get("totalChunks")).intValue();
        if (index < 0 || index >= total) {
            throw new IllegalArgumentException("分片序号越界！");
        }
        long chunkSize = ((Number) meta.get("chunkSize")).longValue();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("分片内容不能为空！");
        }
        if (file.getSize() > chunkSize) {
            throw new IllegalArgumentException("分片大小超出声明值！");
        }
        Path chunkPath = CHUNK_DIR.resolve(uploadId).resolve(String.valueOf(index));
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, chunkPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("分片写入失败", e);
        }
        //更新已传索引（重复上传同一片按已处理，幂等）
        List<Number> uploaded = (List<Number>) meta.get("uploaded");
        if (uploaded.stream().noneMatch(i -> i.intValue() == index)) {
            uploaded.add(index);
            saveMeta(meta);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> complete(String uploadId, String username) {
        Map<String, Object> meta = loadMeta(uploadId, username);
        int total = ((Number) meta.get("totalChunks")).intValue();
        Path taskDir = CHUNK_DIR.resolve(uploadId);
        //1、分片齐全性校验（以磁盘实际存在为准，Redis 记录仅作展示）
        for (int i = 0; i < total; i++) {
            if (!Files.isRegularFile(taskDir.resolve(String.valueOf(i)))) {
                throw new IllegalArgumentException("分片不完整（缺少第 " + i + " 片），请续传后重试！");
            }
        }
        //2、合并
        byte[] bytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < total; i++) {
                out.write(Files.readAllBytes(taskDir.resolve(String.valueOf(i))));
            }
            bytes = out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("分片合并失败", e);
        }
        long expected = ((Number) meta.get("fileSize")).longValue();
        if (bytes.length != expected) {
            cleanup(uploadId, meta);
            throw new IllegalArgumentException("合并后大小与声明不一致，请重新上传！");
        }
        //3、服务端 MD5 复核（客户端 MD5 仅作秒传提示）
        String actualMd5 = md5Hex(bytes);
        String clientMd5 = (String) meta.get("md5");
        if (clientMd5 != null && !clientMd5.equalsIgnoreCase(actualMd5)) {
            cleanup(uploadId, meta);
            throw new IllegalArgumentException("文件 MD5 校验不一致，上传内容可能损坏，请重新上传！");
        }
        //4、统一落存储（白名单+魔数+配额+秒传+对象去重全套）
        FileInfo info = fileInfoService.store(bytes, (String) meta.get("fileName"),
                (String) meta.get("bizType"), username, (String) meta.get("folderId"));
        //5、清理临时数据
        cleanup(uploadId, meta);
        Map<String, Object> result = new HashMap<>();
        result.put("instant", false);
        result.put("file", buildSummary(info));
        return result;
    }

    @Override
    public void clearExpired() {
        Set<String> keys = redisUtil.scanKeys(KEY_PREFIX + "*");
        int cleaned = 0;
        for (String key : keys) {
            try {
                String json = (String) redisUtil.get(key);
                if (json == null) {
                    continue;
                }
                Map<String, Object> meta = JsonUtils.parseToMap(json);
                String createTime = (String) meta.get("createTime");
                LocalDateTime created = createTime != null ? LocalDateTime.parse(createTime) : null;
                if (created == null || Duration.between(created, LocalDateTime.now()).compareTo(TASK_TTL) > 0) {
                    cleanup((String) meta.get("uploadId"), meta);
                    cleaned++;
                }
            } catch (Exception e) {
                log.warn("分片任务清理失败 key={}", key, e);
            }
        }
        //孤儿目录兜底（Redis 键已过期但目录残留：键 get 为空即任务已过期/不存在）
        if (Files.isDirectory(CHUNK_DIR)) {
            try (var stream = Files.list(CHUNK_DIR)) {
                stream.forEach(p -> {
                    String json = (String) redisUtil.get(KEY_PREFIX + p.getFileName().toString());
                    if (json == null) {
                        deleteRecursively(p);
                    }
                });
            } catch (Exception e) {
                log.warn("分片孤儿目录清理失败", e);
            }
        }
        if (cleaned > 0) {
            log.info("分片上传超期任务清理完成：{} 个", cleaned);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 断点续传探测：同用户+同文件名+同大小的未过期任务
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findResumableTask(String username, String fileName, long fileSize) {
        Set<String> keys = redisUtil.scanKeys(KEY_PREFIX + "*");
        for (String key : keys) {
            try {
                String json = (String) redisUtil.get(key);
                if (json == null) {
                    continue;
                }
                Map<String, Object> meta = JsonUtils.parseToMap(json);
                if (username.equals(meta.get("username")) && fileName.equals(meta.get("fileName"))
                        && ((Number) meta.get("fileSize")).longValue() == fileSize) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("instant", false);
                    result.put("uploadId", meta.get("uploadId"));
                    result.put("uploaded", meta.get("uploaded"));
                    return result;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Map<String, Object> loadMeta(String uploadId, String username) {
        if (uploadId == null || uploadId.isBlank() || !uploadId.matches("^[0-9a-f]{32}$")) {
            throw new IllegalArgumentException("uploadId 非法！");
        }
        String json = (String) redisUtil.get(KEY_PREFIX + uploadId);
        if (json == null) {
            throw new IllegalArgumentException("分片任务不存在或已过期，请重新上传！");
        }
        Map<String, Object> meta = JsonUtils.parseToMap(json);
        if (!username.equals(meta.get("username"))) {
            throw new IllegalArgumentException("无权操作他人上传任务！");
        }
        return meta;
    }

    private void saveMeta(Map<String, Object> meta) {
        redisUtil.set(KEY_PREFIX + meta.get("uploadId"), JsonUtils.toJson(meta), TASK_TTL.toSeconds());
    }

    private void cleanup(String uploadId, Map<String, Object> meta) {
        try {
            redisUtil.del(KEY_PREFIX + uploadId);
        } catch (Exception ignored) {
        }
        if (uploadId != null) {
            deleteRecursively(CHUNK_DIR.resolve(uploadId));
        }
    }

    private Map<String, Object> buildSummary(FileInfo info) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", info.getId());
        summary.put("name", info.getOriginalName());
        summary.put("ext", info.getExt());
        summary.put("size", info.getFileSize());
        return summary;
    }

    private static String md5Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }

    private static void deleteRecursively(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
        } catch (Exception ignored) {
        }
    }
}
