package com.xuanchen.filemanage.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.filemanage.entity.FileCollaborator;

import java.util.List;

/**
 * Service接口-->文件协作成员
 *
 * @author XuanChen
 * @date 2026-10-28
 */
public interface IFileCollaboratorService extends IService<FileCollaborator> {

    /**
     * 查询文件的协作者列表（仅属主或 admin 可查）
     *
     * @param fileId   文件id
     * @param username 当前登录名
     * @param isAdmin  当前用户是否 admin
     * @return 协作者列表（按添加时间倒序）
     */
    List<FileCollaborator> listByFile(String fileId, String username, boolean isAdmin);

    /**
     * 添加协作者（仅属主或 admin 可操作；目标必须是平台内有效账号且非属主本人、不重复）
     *
     * @param fileId     文件id
     * @param targetUser 目标协作者登录名
     * @param permission 1=可编辑 2=只读
     * @param username   当前登录名（属主）
     * @param isAdmin    当前用户是否 admin
     */
    void add(String fileId, String targetUser, Integer permission, String username, boolean isAdmin);

    /**
     * 移除协作者（仅属主或 admin 可操作；不存在时静默成功）
     *
     * @param fileId     文件id
     * @param targetUser 目标协作者登录名
     * @param username   当前登录名（属主）
     * @param isAdmin    当前用户是否 admin
     */
    void remove(String fileId, String targetUser, String username, boolean isAdmin);

    /**
     * 查询用户对文件的协作权限
     *
     * @param fileId   文件id
     * @param username 协作者登录名
     * @return 1=可编辑 2=只读；非协作者返回 null
     */
    Integer permissionOf(String fileId, String username);
}
