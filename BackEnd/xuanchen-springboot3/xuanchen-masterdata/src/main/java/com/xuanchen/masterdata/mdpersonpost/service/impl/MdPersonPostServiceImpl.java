package com.xuanchen.masterdata.mdpersonpost.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.masterdata.mdpersonpost.entity.MdPersonPost;
import com.xuanchen.masterdata.mdpersonpost.mapper.MdPersonPostMapper;
import com.xuanchen.masterdata.mdpersonpost.service.IMdPersonPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service接口实现类-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Service
public class MdPersonPostServiceImpl extends ServiceImpl<MdPersonPostMapper, MdPersonPost> implements IMdPersonPostService {
    @Autowired
    private MdPersonPostMapper mdPersonPostMapper;

    @Override
    public IPage<MdPersonPost> listAssignUser(Page<MdPersonPost> page, String postId) {
        return page.setRecords(mdPersonPostMapper.listAssignUser(page, postId));
    }

    @Override
    public IPage<MdPersonPost> listUnAssignUser(Page<MdPersonPost> page, String postId) {
        return page.setRecords(mdPersonPostMapper.listUnAssignUser(page, postId));
    }

    /**
     * 添加 用户 岗位 对应关系
     *
     * @param personId
     * @param postIds
     * @return
     */
    @Override
    public Boolean add(String personId, String[] postIds) {
        if (postIds == null) {
            return false;
        }
        List<MdPersonPost> list = new ArrayList<>();
        for (String postId : postIds) {
            MdPersonPost mdPersonPost = new MdPersonPost();
            mdPersonPost.setPersonId(personId);
            mdPersonPost.setPostId(postId);
            list.add(mdPersonPost);
        }
        return saveBatch(list);
    }
}
