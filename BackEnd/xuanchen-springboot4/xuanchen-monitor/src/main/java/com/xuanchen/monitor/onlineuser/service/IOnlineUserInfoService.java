package com.xuanchen.monitor.onlineuser.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;

/**
 * Service接口-->在线用户
 *
 * @author XuanChen
 * @date 2026-01-29
 */
public interface IOnlineUserInfoService extends IService<OnlineUserInfo> {
    /**
     * 分页列表
     *
     * @param page
     * @return
     */
    IPage<OnlineUserInfo> list(Page<OnlineUserInfo> page);
}
