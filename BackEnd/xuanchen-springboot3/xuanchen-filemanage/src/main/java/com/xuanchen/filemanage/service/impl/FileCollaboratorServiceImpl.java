package com.xuanchen.filemanage.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.filemanage.entity.FileCollaborator;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.mapper.FileCollaboratorMapper;
import com.xuanchen.filemanage.mapper.FileInfoMapper;
import com.xuanchen.filemanage.service.IFileCollaboratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service接口实现类-->文件协作成员
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Slf4j
@Service
public class FileCollaboratorServiceImpl extends ServiceImpl<FileCollaboratorMapper, FileCollaborator>
        implements IFileCollaboratorService {

    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    @Override
    public List<FileCollaborator> listByFile(String fileId, String username, boolean isAdmin) {
        requireOwner(fileId, username, isAdmin);
        return this.lambdaQuery().eq(FileCollaborator::getFileId, fileId)
                .orderByDesc(FileCollaborator::getCreateTime).list();
    }

    @Override
    public void add(String fileId, String targetUser, Integer permission, String username, boolean isAdmin) {
        FileInfo file = requireOwner(fileId, username, isAdmin);
        if (targetUser == null || targetUser.isBlank()) {
            throw new IllegalArgumentException("请填写协作者账号！");
        }
        if (permission == null || (permission != 1 && permission != 2)) {
            throw new IllegalArgumentException("协作权限非法（1=可编辑 2=只读）！");
        }
        if (targetUser.equals(file.getUsername())) {
            throw new IllegalArgumentException("文件属主无需添加为协作者！");
        }
        //目标必须是平台内有效账号（未删除且未停用）
        Map<String, Object> target = authServiceCommon.getUserByUserName(targetUser);
        if (target == null
                || !Integer.valueOf(0).equals(toInt(target.get("delFlag")))
                || !Integer.valueOf(1).equals(toInt(target.get("status")))) {
            throw new IllegalArgumentException("协作者账号不存在或已被禁用！");
        }
        if (this.lambdaQuery().eq(FileCollaborator::getFileId, fileId)
                .eq(FileCollaborator::getUsername, targetUser).count() > 0) {
            throw new IllegalArgumentException("该用户已是协作者，请勿重复添加！");
        }
        FileCollaborator collab = new FileCollaborator();
        collab.setId(UUID.randomUUID().toString().replace("-", ""));
        collab.setFileId(fileId);
        collab.setUsername(targetUser);
        collab.setPermission(permission);
        collab.setCreateBy(username);
        collab.setCreateTime(LocalDateTime.now());
        this.save(collab);
        log.info("添加文件协作者 fileId={} target={} permission={} operator={}", fileId, targetUser, permission,
                username);
    }

    @Override
    public void remove(String fileId, String targetUser, String username, boolean isAdmin) {
        requireOwner(fileId, username, isAdmin);
        this.lambdaUpdate().eq(FileCollaborator::getFileId, fileId)
                .eq(FileCollaborator::getUsername, targetUser).remove();
        log.info("移除文件协作者 fileId={} target={} operator={}", fileId, targetUser, username);
    }

    @Override
    public Integer permissionOf(String fileId, String username) {
        FileCollaborator collab = this.lambdaQuery().eq(FileCollaborator::getFileId, fileId)
                .eq(FileCollaborator::getUsername, username).last("LIMIT 1").one();
        return collab == null ? null : collab.getPermission();
    }

    /**
     * 校验文件存在且操作人是属主（或 admin），返回文件元数据
     */
    private FileInfo requireOwner(String fileId, String username, boolean isAdmin) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在或已删除！");
        }
        if (!isAdmin && !username.equals(file.getUsername())) {
            throw new IllegalArgumentException("仅文件属主可管理协作者！");
        }
        return file;
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
