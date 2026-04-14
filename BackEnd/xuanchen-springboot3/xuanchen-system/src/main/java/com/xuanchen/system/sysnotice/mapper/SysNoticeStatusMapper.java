package com.xuanchen.system.sysnotice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysnotice.entity.SysNoticeStatus;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper --> 通知 阅读状态
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Repository
public interface SysNoticeStatusMapper extends BaseMapper<SysNoticeStatus> {
    /**
     * 分页列表
     *
     * @param page
     * @param sysNoticeStatus
     * @return
     */
    List<SysNoticeStatus> list(@Param("page") Page<SysNoticeStatus> page, @Param("sysNoticeStatus") SysNoticeStatus sysNoticeStatus);

    /**
     * 获取用户未读通知数量
     *
     * @param userId
     * @return
     */
    Integer getNoticeCountByUserName(String userId);
}

