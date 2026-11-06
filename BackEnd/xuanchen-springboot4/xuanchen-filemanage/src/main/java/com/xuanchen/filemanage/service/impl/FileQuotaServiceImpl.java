package com.xuanchen.filemanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.filemanage.entity.FileQuota;
import com.xuanchen.filemanage.mapper.FileInfoMapper;
import com.xuanchen.filemanage.mapper.FileQuotaMapper;
import com.xuanchen.filemanage.service.IFileQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service接口实现类-->文件中心用户配额（二期）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Service
public class FileQuotaServiceImpl extends ServiceImpl<FileQuotaMapper, FileQuota> implements IFileQuotaService {

    /**
     * 登录名约束（宽松：字母数字与常见邮箱符号，2~64 位）
     */
    private static final java.util.regex.Pattern USERNAME_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_@.\\-]{2,64}$");

    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 未单独设置配额的用户使用的系统默认值（字节，0=不限制）
     */
    @Value("${xuanchen.fileinfo.default-quota-bytes:1073741824}")
    private long defaultQuotaBytes;

    @Override
    public long getEffectiveQuotaBytes(String username) {
        if (username == null || username.isBlank()) {
            return defaultQuotaBytes;
        }
        FileQuota quota = this.lambdaQuery().eq(FileQuota::getUsername, username).one();
        return quota != null && quota.getQuotaBytes() != null ? quota.getQuotaBytes() : defaultQuotaBytes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQuota(String username, long quotaBytes, String operator) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("登录名非法！");
        }
        if (quotaBytes < -1) {
            throw new IllegalArgumentException("配额值非法！");
        }
        //-1 = 恢复默认（删除专属记录）
        if (quotaBytes == -1) {
            this.lambdaUpdate().eq(FileQuota::getUsername, username).remove();
            return;
        }
        FileQuota quota = this.lambdaQuery().eq(FileQuota::getUsername, username).one();
        if (quota == null) {
            quota = new FileQuota();
            quota.setUsername(username);
            quota.setQuotaBytes(quotaBytes);
            quota.setCreateBy(operator);
            quota.setCreateTime(LocalDateTime.now());
            this.save(quota);
        } else {
            quota.setQuotaBytes(quotaBytes);
            quota.setUpdateBy(operator);
            quota.setUpdateTime(LocalDateTime.now());
            this.updateById(quota);
        }
    }

    @Override
    public List<Map<String, Object>> listQuotaUsage() {
        return baseMapper.listQuotaUsage();
    }

    @Override
    public Map<String, Object> myUsage(String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("usedBytes", fileInfoMapper.sumUsedBytes(username));
        data.put("quotaBytes", getEffectiveQuotaBytes(username));
        return data;
    }
}
