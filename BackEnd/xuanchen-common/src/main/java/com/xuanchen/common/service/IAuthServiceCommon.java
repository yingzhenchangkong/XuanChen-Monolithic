package com.xuanchen.common.service;

import java.util.Map;

/**
 * Service接口-->认证、授权 通用
 *
 * @author XuanChen
 * @date 2026-02-05
 */
public interface IAuthServiceCommon {
    /**
     * 加密密码
     *
     * @param password
     * @param salt
     * @return
     */
    String encryptPassword(String password, String salt);

    /**
     * 获取用户名
     *
     * @param token
     * @return
     */
    String getUserNameByToken(String token);

    /**
     * 获取用户信息
     *
     * @param userName
     * @return
     */
    Map<String, Object> getUserByUserName(String userName);
}
