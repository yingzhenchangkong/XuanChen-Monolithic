package com.xuanchen.masterdata.mdpost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.masterdata.mdpost.entity.MdPost;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->岗位
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Repository
public interface MdPostMapper extends BaseMapper<MdPost> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param mdPost
     * @return
     */
    List<MdPost> listRecycleBin(@Param("page") Page<MdPost> page, @Param("mdPost") MdPost mdPost);

    /**
     * 回收站 彻底删除
     *
     * @param mdPostIds
     * @return
     */
    Integer deleteRecycleBin(@Param("mdPostIds") String[] mdPostIds);

    /**
     * 回收站 还原
     *
     * @param mdPostIds
     * @return
     */
    Integer revertRecycleBin(@Param("mdPostIds") String[] mdPostIds);

    List<MdPost> ifExistsId(@Param("mdPost") MdPost mdPost);

    List<MdPost> ifExistsNoId(@Param("mdPost") MdPost mdPost);
}