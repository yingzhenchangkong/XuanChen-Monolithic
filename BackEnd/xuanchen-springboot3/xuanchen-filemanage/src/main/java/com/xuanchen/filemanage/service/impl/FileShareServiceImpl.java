package com.xuanchen.filemanage.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.entity.FileShare;
import com.xuanchen.filemanage.mapper.FileShareMapper;
import com.xuanchen.filemanage.service.IFileInfoService;
import com.xuanchen.filemanage.service.IFileShareService;
import com.xuanchen.filemanage.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service接口实现类-->文件共享（二期）
 * 安全约束：仅文件归属人可创建；公开访问须通过 share_no（32 位随机 UUID，不可枚举）
 * + 可选提取码，且源文件必须仍有效；下载一律现签预签名 URL（30 分钟自动失效）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements IFileShareService {

    /**
     * 提取码：4~8 位字母数字
     */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,8}$");

    @Autowired
    private IFileInfoService fileInfoService;

    @Autowired
    private StorageService storageService;

    @Override
    public FileShare create(String fileId, Integer expireHours, String shareCode, String username) {
        FileInfo info = fileInfoService.getById(fileId);
        if (info == null) {
            throw new IllegalArgumentException("文件不存在或已被删除！");
        }
        if (info.getUsername() == null || !info.getUsername().equals(username)) {
            throw new IllegalArgumentException("只能分享自己的文件！");
        }
        if (expireHours == null || expireHours < 1 || expireHours > 720) {
            throw new IllegalArgumentException("有效期须在 1 小时 ~ 30 天之间！");
        }
        String code = null;
        if (shareCode != null && !shareCode.isBlank()) {
            code = shareCode.trim();
            if (!CODE_PATTERN.matcher(code).matches()) {
                throw new IllegalArgumentException("提取码须为 4~8 位字母或数字！");
            }
        }
        FileShare share = new FileShare();
        share.setShareNo(UUID.randomUUID().toString().replace("-", ""));
        share.setFileId(fileId);
        share.setUsername(username);
        share.setShareCode(code);
        share.setExpireTime(LocalDateTime.now().plusHours(expireHours));
        share.setVisitCount(0);
        share.setDelFlag(0);
        share.setCreateTime(LocalDateTime.now());
        this.save(share);
        return share;
    }

    @Override
    public List<FileShare> listMine(String username) {
        return this.lambdaQuery().eq(FileShare::getUsername, username)
                .orderByDesc(FileShare::getCreateTime).list();
    }

    @Override
    public void revoke(String id, String username, boolean isAdmin) {
        FileShare share = this.getById(id);
        if (share == null) {
            throw new IllegalArgumentException("分享不存在！");
        }
        if (!isAdmin && (share.getUsername() == null || !share.getUsername().equals(username))) {
            throw new IllegalArgumentException("无权撤销他人分享！");
        }
        this.removeById(id);
    }

    @Override
    public Map<String, Object> resolve(String shareNo, String code, boolean wantUrl) {
        if (shareNo == null || shareNo.isBlank() || !shareNo.matches("^[a-f0-9]{32}$")) {
            throw new IllegalArgumentException("分享链接无效！");
        }
        FileShare share = this.lambdaQuery().eq(FileShare::getShareNo, shareNo).one();
        if (share == null) {
            throw new IllegalArgumentException("分享不存在或已被撤销！");
        }
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("分享链接已过期！");
        }
        if (share.getShareCode() != null && !share.getShareCode().isBlank()) {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("此分享需要提取码！");
            }
            if (!share.getShareCode().equalsIgnoreCase(code.trim())) {
                throw new IllegalArgumentException("提取码错误！");
            }
        }
        FileInfo info = fileInfoService.getById(share.getFileId());
        if (info == null) {
            throw new IllegalArgumentException("分享的文件已不存在！");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("name", info.getOriginalName());
        data.put("ext", info.getExt());
        data.put("size", info.getFileSize());
        data.put("expireTime", share.getExpireTime());
        data.put("hasCode", share.getShareCode() != null && !share.getShareCode().isBlank());
        if (wantUrl) {
            //下载次数累加（失败不影响下载主流程）
            try {
                this.lambdaUpdate().eq(FileShare::getId, share.getId())
                        .setSql("visit_count = COALESCE(visit_count, 0) + 1").update();
            } catch (Exception e) {
                log.warn("分享下载计数失败 id={}", share.getId(), e);
            }
            data.put("url", storageService.previewUrl(info.getBucket(), info.getObjectKey(),
                    info.getOriginalName(), false, info.getContentType()));
        }
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int revokeExpired() {
        List<FileShare> expired = this.lambdaQuery()
                .eq(FileShare::getDelFlag, 0)
                .lt(FileShare::getExpireTime, LocalDateTime.now())
                .list();
        for (FileShare share : expired) {
            this.removeById(share.getId());
        }
        return expired.size();
    }
}
