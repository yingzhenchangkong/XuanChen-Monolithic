package com.xuanchen.filemanage.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.filemanage.entity.FileQuota;

import java.util.List;
import java.util.Map;

/**
 * Service接口-->文件中心用户配额（二期）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IFileQuotaService extends IService<FileQuota> {

    /**
     * 用户生效配额（字节）：有专属记录取记录值，否则取系统默认；0=不限制
     */
    long getEffectiveQuotaBytes(String username);

    /**
     * 保存用户配额（admin）：quotaBytes=-1 表示恢复默认（删除专属记录）
     */
    void saveQuota(String username, long quotaBytes, String operator);

    /**
     * 配额用量总览（admin）：[{username, quotaBytes(-1=默认), usedBytes}]
     */
    List<Map<String, Object>> listQuotaUsage();

    /**
     * 我的用量：{usedBytes, quotaBytes(生效值)}
     */
    Map<String, Object> myUsage(String username);
}
