package com.xuanchen.system.sysuserpost.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysuserpost.entity.SysUserPost;
import com.xuanchen.system.sysuserpost.mapper.SysUserPostMapper;
import com.xuanchen.system.sysuserpost.service.ISysUserPostService;
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
public class SysUserPostServiceImpl extends ServiceImpl<SysUserPostMapper, SysUserPost> implements ISysUserPostService {
    /**
     * 添加 用户 岗位 对应关系
     *
     * @param userId
     * @param postIds
     * @return
     */
    @Override
    public Boolean add(String userId, String[] postIds) {
        if (postIds == null) {
            return false;
        }
        List<SysUserPost> list = new ArrayList<>();
        for (String postId : postIds) {
            SysUserPost sysUserPost = new SysUserPost();
            sysUserPost.setUserId(userId);
            sysUserPost.setPostId(postId);
            list.add(sysUserPost);
        }
        return saveBatch(list);
    }
}
