package com.xuanchen.online.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.online.entity.OnlineMindmap;

/**
 * 服务-->在线文档-思维导图
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IOnlineMindmapService extends IService<OnlineMindmap> {

    /**
     * 分页查询（本人全部 + admin 全部，按更新时间倒序）
     *
     * @param pageNo   页码（1 起）
     * @param pageSize 页大小
     * @param keyword  文档名模糊筛选，可空
     * @param username 当前登录名
     * @param isAdmin  是否管理员
     */
    Page<OnlineMindmap> pageList(long pageNo, long pageSize, String keyword, String username, boolean isAdmin);

    /**
     * 详情（本人或 admin）
     */
    OnlineMindmap getDetail(String id, String username, boolean isAdmin);

    /**
     * 保存（新建或更新；更新需本人或 admin）
     *
     * @return 保存后的实体（含 id）
     */
    OnlineMindmap saveDoc(String id, String docName, String snapshot, String username, boolean isAdmin);

    /**
     * 删除（逻辑删，本人或 admin）
     */
    void deleteDoc(String id, String username, boolean isAdmin);
}
