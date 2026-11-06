package com.xuanchen.filemanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.filemanage.entity.FileQuota;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Mapper-->文件中心用户配额
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Repository
public interface FileQuotaMapper extends BaseMapper<FileQuota> {

    /**
     * 配额用量总览：出现在 file_info 或 file_quota 中的用户，
     * quotaBytes=-1 表示未单独设置（使用系统默认配额）。仅 admin 配额管理页使用。
     */
    @Select("SELECT u.username AS username, COALESCE(q.quota_bytes, -1) AS quotaBytes,"
            + " COALESCE(f.used_bytes, 0) AS usedBytes"
            + " FROM (SELECT DISTINCT username FROM file_info WHERE username IS NOT NULL"
            + "       UNION SELECT username FROM file_quota) u"
            + " LEFT JOIN file_quota q ON q.username = u.username"
            + " LEFT JOIN (SELECT username, SUM(file_size) AS used_bytes FROM file_info"
            + "            WHERE del_flag = 0 GROUP BY username) f ON f.username = u.username"
            + " ORDER BY usedBytes DESC")
    List<Map<String, Object>> listQuotaUsage();
}
