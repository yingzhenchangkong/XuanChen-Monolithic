package com.xuanchen.filemanage.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Service接口-->分片上传/断点续传
 * 进度元数据存 Redis（TTL 24h），分片落临时目录；complete 时合并→统一走
 * IFileInfoService.store 落存储（复用白名单/魔数/配额/秒传/对象去重全套校验）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IFileChunkService {

    /**
     * 初始化分片任务
     *
     * @param fileName    原始文件名
     * @param fileSize    文件总字节数
     * @param md5         文件整体 MD5（前端 spark-md5 预计算，服务端 complete 时复核）
     * @param bizType     业务类型
     * @param folderId    目标文件夹id（空=根目录）
     * @param totalChunks 分片总数
     * @param chunkSize   单片大小（字节，末片可小）
     * @param username    上传人登录名
     * @return {instant, file?, uploadId?, uploaded?}：秒传命中返回 instant=true+file；
     *         命中断点续传/新建任务返回 uploadId + uploaded（已传分片索引）
     */
    Map<String, Object> init(String fileName, long fileSize, String md5, String bizType,
                             String folderId, int totalChunks, long chunkSize, String username);

    /**
     * 保存单片
     *
     * @param uploadId 任务id
     * @param index    分片序号（0 起）
     * @param file     分片内容
     * @param username 上传人登录名（归属校验）
     */
    void saveChunk(String uploadId, int index, MultipartFile file, String username);

    /**
     * 完成合并：分片齐全性校验 → 合并 → 服务端 MD5 复核 → 统一落存储 → 清理临时数据
     *
     * @return 落库后的文件元数据摘要
     */
    Map<String, Object> complete(String uploadId, String username);

    /**
     * 定时清理：超期（24h）未完成的任务（Redis 元数据 + 临时目录）
     */
    void clearExpired();
}
