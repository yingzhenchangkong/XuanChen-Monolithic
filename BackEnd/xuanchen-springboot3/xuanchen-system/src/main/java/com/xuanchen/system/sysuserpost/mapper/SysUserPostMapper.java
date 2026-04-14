package com.xuanchen.system.sysuserpost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysuserpost.entity.SysUserPost;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Repository
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {
    List<SysUserPost> listAssignUser(@Param("page") Page<SysUserPost> page, @Param("postId") String postId);

    List<SysUserPost> listUnAssignUser(@Param("page") Page<SysUserPost> page, @Param("postId") String postId);
}
