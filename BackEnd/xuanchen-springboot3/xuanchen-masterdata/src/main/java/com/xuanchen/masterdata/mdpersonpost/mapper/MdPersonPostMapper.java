package com.xuanchen.masterdata.mdpersonpost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.masterdata.mdpersonpost.entity.MdPersonPost;
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
public interface MdPersonPostMapper extends BaseMapper<MdPersonPost> {
    List<MdPersonPost> listAssignUser(@Param("page") Page<MdPersonPost> page, @Param("postId") String postId);

    List<MdPersonPost> listUnAssignUser(@Param("page") Page<MdPersonPost> page, @Param("postId") String postId);
}
