package com.xuanchen.filemanage.storage;

import com.xuanchen.common.utils.FileSecurityUtil;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 存储实现-->本地磁盘（缺省兜底，兼容存量头像/本地文件）
 * 与 FileManageController 共用 xuanchen.path.upload 根目录，落盘路径必须经
 * FileSecurityUtil.resolveUnderRoot 校验，防止路径穿越。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "xuanchen.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${xuanchen.path.upload}")
    private String uploadPath;

    private Path uploadRoot;

    @PostConstruct
    public void init() {
        uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
            log.info("本地存储根目录初始化成功: {}", uploadRoot);
        } catch (Exception e) {
            log.error("本地存储根目录不可用: {}，请检查 xuanchen.path.upload 配置与目录权限", uploadRoot, e);
        }
    }

    @Override
    public void put(String bucket, String objectKey, InputStream in, long size, String contentType) {
        //本地实现桶仅作逻辑分组，忽略 bucket，统一落在上传根目录下
        Path target;
        try {
            target = FileSecurityUtil.resolveUnderRoot(uploadRoot, objectKey);
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new StorageException("文件保存失败：" + e.getMessage(), e);
        }
    }

    @Override
    public InputStream get(String bucket, String objectKey) {
        try {
            Path target = resolveSafe(objectKey);
            return Files.newInputStream(target);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("文件读取失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String previewUrl(String bucket, String objectKey, String downloadName, boolean inline,
                             String contentType) {
        //本地实现返回内部静态地址（FileManageController#view 已按图片 inline、其余 attachment 处理）
        String encoded = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
        return "/filemanage/static/" + objectKey + "?name=" + encoded + "&inline=" + inline;
    }

    @Override
    public void remove(String bucket, String objectKey) {
        try {
            Path target = resolveSafe(objectKey);
            Files.deleteIfExists(target);
        } catch (Exception e) {
            throw new StorageException("文件删除失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        try {
            return Files.isRegularFile(resolveSafe(objectKey));
        } catch (Exception e) {
            return false;
        }
    }

    private Path resolveSafe(String objectKey) {
        Path target = FileSecurityUtil.resolveUnderRoot(uploadRoot, objectKey);
        try {
            if (!target.toRealPath().startsWith(uploadRoot.toRealPath())) {
                throw new StorageException("非法路径：禁止访问上传目录之外的文件", null);
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("文件路径解析失败", e);
        }
        return target;
    }
}
