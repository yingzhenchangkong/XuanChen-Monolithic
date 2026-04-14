package com.xuanchen.system.sysuserpost.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysuserpost.entity.SysUserPost;

/**
 * Service接口-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
public interface ISysUserPostService extends IService<SysUserPost> {
    IPage<SysUserPost> listAssignUser(Page<SysUserPost> page, String postId);

    IPage<SysUserPost> listUnAssignUser(Page<SysUserPost> page, String postId);

    Boolean add(String userId, String[] postIds);
}
