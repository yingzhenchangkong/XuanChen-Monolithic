package com.xuanchen.filemanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.filemanage.entity.FileFolder;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.mapper.FileFolderMapper;
import com.xuanchen.filemanage.service.IFileFolderService;
import com.xuanchen.filemanage.service.IFileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service接口实现类-->文件中心文件夹
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@Service
public class FileFolderServiceImpl extends ServiceImpl<FileFolderMapper, FileFolder> implements IFileFolderService {

    /**
     * FileInfoService 依赖 FolderService（restore 校验），FolderService 又删文件，
     * 加 @Lazy 打破循环依赖
     */
    private final IFileInfoService fileInfoService;

    public FileFolderServiceImpl(@Lazy IFileInfoService fileInfoService) {
        this.fileInfoService = fileInfoService;
    }

    @Override
    public List<FileFolder> listMine(String username) {
        QueryWrapper<FileFolder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username).orderByAsc("folder_name");
        return this.list(queryWrapper);
    }

    @Override
    public FileFolder create(String parentId, String folderName, String username) {
        String name = normalizeName(folderName);
        String parent = normalizeParent(parentId);
        if (!"0".equals(parent)) {
            FileFolder parentFolder = this.getById(parent);
            if (parentFolder == null || !username.equals(parentFolder.getUsername())) {
                throw new IllegalArgumentException("父文件夹不存在或无权操作！");
            }
        }
        checkSiblingNameDup(parent, name, username, null);
        FileFolder folder = new FileFolder();
        folder.setFolderName(name);
        folder.setParentId(parent);
        folder.setUsername(username);
        folder.setCreateBy(username);
        folder.setCreateTime(LocalDateTime.now());
        this.save(folder);
        return folder;
    }

    @Override
    public void rename(String id, String folderName, String username) {
        String name = normalizeName(folderName);
        FileFolder folder = this.getById(id);
        if (folder == null || !username.equals(folder.getUsername())) {
            throw new IllegalArgumentException("文件夹不存在或无权操作！");
        }
        checkSiblingNameDup(folder.getParentId(), name, username, id);
        this.lambdaUpdate().eq(FileFolder::getId, id)
                .set(FileFolder::getFolderName, name)
                .set(FileFolder::getUpdateBy, username)
                .set(FileFolder::getUpdateTime, LocalDateTime.now())
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, String username) {
        FileFolder folder = this.getById(id);
        if (folder == null || !username.equals(folder.getUsername())) {
            throw new IllegalArgumentException("文件夹不存在或无权操作！");
        }
        //1、递归收集后代（含自身），全部逻辑删
        List<String> ids = baseMapper.listDescendantIds(id, username);
        this.lambdaUpdate().in(FileFolder::getId, ids)
                .set(FileFolder::getDelFlag, 1)
                .set(FileFolder::getUpdateBy, username)
                .set(FileFolder::getUpdateTime, LocalDateTime.now())
                .update();
        //2、这些文件夹下的有效文件整体移入回收站（复用回收站语义，deleteTime 直接落库）
        fileInfoService.lambdaUpdate()
                .in(FileInfo::getFolderId, ids)
                .eq(FileInfo::getDelFlag, 0)
                .set(FileInfo::getDelFlag, 1)
                .set(FileInfo::getDeleteTime, LocalDateTime.now())
                .update();
        log.info("文件夹删除完成 username={} folder={} 子树={} (文件已移入回收站)", username, folder.getFolderName(), ids.size());
    }

    @Override
    public boolean existsValid(String folderId, String username) {
        if (folderId == null || folderId.isBlank()) {
            return true;
        }
        FileFolder folder = this.getById(folderId);
        return folder != null && username.equals(folder.getUsername());
    }

    private void checkSiblingNameDup(String parentId, String name, String username, String excludeId) {
        QueryWrapper<FileFolder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username).eq("parent_id", parentId).eq("folder_name", name);
        if (excludeId != null) {
            queryWrapper.ne("id", excludeId);
        }
        if (this.count(queryWrapper) > 0) {
            throw new IllegalArgumentException("同级目录下已存在同名文件夹！");
        }
    }

    private static String normalizeName(String folderName) {
        if (folderName == null || folderName.isBlank()) {
            throw new IllegalArgumentException("文件夹名不能为空！");
        }
        String name = folderName.strip();
        if (name.length() > 64) {
            throw new IllegalArgumentException("文件夹名过长（最大 64 字符）！");
        }
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new IllegalArgumentException("文件夹名含非法字符！");
        }
        return name;
    }

    private static String normalizeParent(String parentId) {
        if (parentId == null || parentId.isBlank() || "0".equals(parentId)) {
            return "0";
        }
        return parentId;
    }
}
