package com.xuanchen.filemanage.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 存储实现-->MinIO 对象存储
 * 按 xuanchen.storage.type=minio 装配；MinIO 不可用时直接抛异常，禁止静默降级落本地，
 * 避免“元数据说在 MinIO、实际在本地”的双写不一致。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "xuanchen.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private final StorageProperties properties;
    private final MinioClient minioClient;

    public MinioStorageService(StorageProperties properties) {
        this.properties = properties;
        StorageProperties.Minio cfg = properties.getMinio();
        this.minioClient = MinioClient.builder()
                .endpoint(cfg.getEndpoint())
                .credentials(cfg.getAccessKey(), cfg.getSecretKey())
                .build();
    }

    /**
     * 启动时探测连通性并确保默认桶存在；失败不阻断启动（部分部署暂不用文件中心），
     * 但必须日志告警，避免首次上传才暴露配置问题。
     */
    @PostConstruct
    public void initBuckets() {
        try {
            ensureBucket(properties.getDefaultBucket());
            ensureBucket(properties.getPublicBucket());
            log.info("MinIO 连接成功: {}，默认桶: {} / 公开桶: {}", properties.getMinio().getEndpoint(),
                    properties.getDefaultBucket(), properties.getPublicBucket());
        } catch (Exception e) {
            log.error("MinIO 连接失败（endpoint={}），文件中心将不可用，请检查服务是否启动: {}",
                    properties.getMinio().getEndpoint(), e.getMessage());
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO 桶不存在，已自动创建: {}", bucket);
        }
    }

    @Override
    public void put(String bucket, String objectKey, InputStream in, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType == null || contentType.isBlank()
                            ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("文件存储失败：" + e.getMessage(), e);
        }
    }

    @Override
    public InputStream get(String bucket, String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            throw new StorageException("文件读取失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String previewUrl(String bucket, String objectKey, String downloadName, boolean inline,
                             String contentType) {
        try {
            //桶私有读，外链一律走预签名；通过响应头参数约束预览/下载行为
            Map<String, String> extra = new HashMap<>();
            String disposition = (inline ? "inline" : "attachment")
                    + "; filename*=UTF-8''" + URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
            extra.put("response-content-disposition", disposition);
            //文本类补 charset：对象元数据里的 text/plain 无编码声明，浏览器直开会按默认编码渲染导致中文乱码
            String textType = withUtf8Charset(contentType);
            if (textType != null) {
                extra.put("response-content-type", textType);
            }
            int expiry = Math.max(1, properties.getMinio().getPresetExpiryMinutes());
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expiry, TimeUnit.MINUTES)
                    .extraQueryParams(extra)
                    .build());
        } catch (Exception e) {
            throw new StorageException("生成访问地址失败：" + e.getMessage(), e);
        }
    }

    /**
     * 文本类 Content-Type 补 charset=UTF-8；非文本或已带 charset 时返回 null（不改写响应头）
     */
    private static String withUtf8Charset(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        boolean text = lower.startsWith("text/")
                || lower.startsWith("application/json")
                || lower.startsWith("application/xml");
        if (!text || lower.contains("charset=")) {
            return null;
        }
        return contentType + "; charset=UTF-8";
    }

    @Override
    public void remove(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            throw new StorageException("文件删除失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return stat != null;
        } catch (Exception e) {
            return false;
        }
    }
}
