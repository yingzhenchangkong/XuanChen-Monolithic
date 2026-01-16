package com.xuanchen.system.syspost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.syspost.entity.SysPost;
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
public interface SysPostMapper extends BaseMapper<SysPost> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysPost
     * @return
     */
    List<SysPost> listRecycleBin(@Param("page") Page<SysPost> page, @Param("sysPost") SysPost sysPost);

    /**
     * 回收站 彻底删除
     *
     * @param sysPostIds
     * @return
     */
    Integer deleteRecycleBin(@Param("sysPostIds") String[] sysPostIds);

    /**
     * 回收站 还原
     *
     * @param sysPostIds
     * @return
     */
    Integer revertRecycleBin(@Param("sysPostIds") String[] sysPostIds);

    List<SysPost> validate(@Param("sysPost") SysPost sysPost);
}