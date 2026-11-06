package com.xuanchen.filemanage.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.filemanage.entity.FileFolder;

import java.util.List;

/**
 * Service接口-->文件中心文件夹
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IFileFolderService extends IService<FileFolder> {

    /**
     * 我的全部有效文件夹（平铺列表，前端组树）
     */
    List<FileFolder> listMine(String username);

    /**
     * 创建文件夹
     *
     * @param parentId   父文件夹id（空或'0'=根目录）
     * @param folderName 文件夹名（1-64 字符，同层重名校验）
     * @param username   归属人登录名
     */
    FileFolder create(String parentId, String folderName, String username);

    /**
     * 重命名（同层重名校验）
     */
    void rename(String id, String folderName, String username);

    /**
     * 删除文件夹：递归全部后代文件夹逻辑删；
     * 其下（含后代文件夹下）所有有效文件移入回收站
     */
    void delete(String id, String username);

    /**
     * 校验文件夹归属有效性：存在、未删、属于该用户
     */
    boolean existsValid(String folderId, String username);
}
