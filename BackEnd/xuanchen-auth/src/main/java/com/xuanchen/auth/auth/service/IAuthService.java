package com.xuanchen.auth.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.auth.auth.entity.Auth;

/**
 * Service接口-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
public interface IAuthService extends IService<Auth> {
    /**
     * 根据用户名获取用户信息
     *
     * @param userName 用户名
     * @return 用户信息
     */
    Auth getByUserName(String userName);
}
