package com.xuanchen.online.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.online.entity.OnlineDiagram;

/**
 * 服务-->在线文档-流程图/架构图
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IOnlineDiagramService extends IService<OnlineDiagram> {

    /**
     * 分页查询（本人全部 + admin 全部，按更新时间倒序）
     *
     * @param pageNo   页码（1 起）
     * @param pageSize 页大小
     * @param keyword  文档名模糊筛选，可空
     * @param username 当前登录名
     * @param isAdmin  是否管理员
     */
    Page<OnlineDiagram> pageList(long pageNo, long pageSize, String keyword, String username, boolean isAdmin);

    /**
     * 详情（本人或 admin）
     */
    OnlineDiagram getDetail(String id, String username, boolean isAdmin);

    /**
     * 保存（新建或更新；更新需本人或 admin）
     *
     * @return 保存后的实体（含 id）
     */
    OnlineDiagram saveDoc(String id, String docName, String snapshot, String username, boolean isAdmin);

    /**
     * 删除（逻辑删，本人或 admin）
     */
    void deleteDoc(String id, String username, boolean isAdmin);
}
