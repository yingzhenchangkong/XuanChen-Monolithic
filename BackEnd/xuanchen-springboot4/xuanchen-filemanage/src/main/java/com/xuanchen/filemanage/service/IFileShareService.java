package com.xuanchen.filemanage.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.filemanage.entity.FileShare;

import java.util.List;
import java.util.Map;

/**
 * Service接口-->文件共享（二期）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IFileShareService extends IService<FileShare> {

    /**
     * 创建共享（仅文件归属人）：返回含 shareNo 的记录
     *
     * @param fileId      文件 id
     * @param expireHours 有效期（小时，1~720）
     * @param shareCode   提取码，可空（4~8 位字母数字）
     * @param username    分享人登录名
     */
    FileShare create(String fileId, Integer expireHours, String shareCode, String username);

    /**
     * 我的共享列表（按创建时间倒序）
     */
    List<FileShare> listMine(String username);

    /**
     * 撤销共享（本人或 admin）
     */
    void revoke(String id, String username, boolean isAdmin);

    /**
     * 公开解析分享（/share/** 免登录接口专用）：
     * 校验存在性/撤销/过期/提取码，源文件须仍有效；
     * wantUrl=true 时累加下载次数并签发下载预签名 URL（inline=false 强制附件下载）。
     *
     * @return {name, ext, size, expireTime, hasCode[, url]}
     */
    Map<String, Object> resolve(String shareNo, String code, boolean wantUrl);

    /**
     * 定时任务：逻辑删已过期的共享记录
     *
     * @return 本次撤销数量
     */
    int revokeExpired();
}
