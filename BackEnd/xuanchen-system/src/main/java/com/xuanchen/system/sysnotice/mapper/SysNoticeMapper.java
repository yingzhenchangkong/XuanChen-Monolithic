package com.xuanchen.system.sysnotice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysnotice.entity.SysNotice;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper --> 通知
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Repository
public interface SysNoticeMapper extends BaseMapper<SysNotice> {
    /**
     * 分页列表(用户)
     *
     * @param page
     * @param sysNotice
     * @return
     */
    List<SysNotice> listUser(@Param("page") Page<SysNotice> page, @Param("sysNotice") SysNotice sysNotice);

    /**
     * 分页列表(管理)
     *
     * @param page
     * @param sysNotice
     * @return
     */
    List<SysNotice> listManage(@Param("page") Page<SysNotice> page, @Param("sysNotice") SysNotice sysNotice);
}

