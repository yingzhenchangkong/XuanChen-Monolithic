package com.xuanchen.masterdata.mdpersonpost.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.masterdata.mdpersonpost.entity.MdPersonPost;

/**
 * Service接口-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
public interface IMdPersonPostService extends IService<MdPersonPost> {
    IPage<MdPersonPost> listAssignUser(Page<MdPersonPost> page, String postId);

    IPage<MdPersonPost> listUnAssignUser(Page<MdPersonPost> page, String postId);

    Boolean add(String personId, String[] postIds);
}
