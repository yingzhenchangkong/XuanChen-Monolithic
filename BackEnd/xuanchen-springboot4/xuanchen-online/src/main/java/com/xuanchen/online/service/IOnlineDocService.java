package com.xuanchen.online.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.filemanage.entity.FileCollaborator;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.online.entity.OnlineDocVO;

import java.util.List;

/**
 * Service接口-->在线协同文档（Word/Excel/PPT，内容落文件中心 file_info）
 *
 * @author XuanChen
 * @date 2026-10-28
 */
public interface IOnlineDocService {

    /**
     * 新建空白文档（POI 内存生成 → 文件中心 store 落库，biz_type='online'）
     *
     * @param docType 文档类型：word/cell/slide
     * @param docName 文档名称（不含扩展名）
     * @param username 当前登录名
     * @return 入库后的文件元数据（含 fileId）
     */
    FileInfo create(String docType, String docName, String username);

    /**
     * 分页列表：本人创建 + 我是协作者（admin 全部），按更新时间倒序
     */
    Page<OnlineDocVO> page(long pageNo, long pageSize, String keyword, String username, boolean isAdmin);

    /**
     * 重命名（仅属主/admin；不改扩展名、不动 update_time，避免影响协同中的 document key）
     */
    void rename(String fileId, String docName, String username, boolean isAdmin);

    /**
     * 删除（仅属主/admin）：走文件中心回收站，协作者记录一并清理
     */
    void remove(String fileId, String username, boolean isAdmin);

    /**
     * 协作者列表（仅属主/admin）
     */
    List<FileCollaborator> collaborators(String fileId, String username, boolean isAdmin);

    /**
     * 添加协作者（仅属主/admin）
     */
    void addCollaborator(String fileId, String targetUser, Integer permission, String username, boolean isAdmin);

    /**
     * 移除协作者（仅属主/admin）
     */
    void removeCollaborator(String fileId, String targetUser, String username, boolean isAdmin);
}
