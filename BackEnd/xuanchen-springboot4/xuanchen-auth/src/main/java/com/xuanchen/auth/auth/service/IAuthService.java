package com.xuanchen.auth.auth.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.auth.auth.entity.Auth;

import java.util.List;

/**
 * Service接口-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
public interface IAuthService extends IService<Auth> {
    /**
     * 根据用户名获取用户信息
     * Auth.delFlag 已标注 {@code @TableLogic}，自动追加 del_flag = 0，
     * 已逻辑删除的账号一律查不到，调用方无需再手写删除标记条件
     *
     * @param userName 用户名
     * @return 用户信息（已删除用户返回 null）
     */
    Auth getByUserName(String userName);

    /**
     * 查询用户已启用角色的角色编码
     *
     * @param userName 用户名
     * @return 角色编码集合
     */
    List<String> listRoleCodesByUserName(String userName);

    /**
     * 查询用户关联菜单的权限标识（perms）
     *
     * @param userName 用户名
     * @return 权限标识集合
     */
    List<String> listPermsByUserName(String userName);

    /**
     * 按 config_key 查询启用中的系统参数值
     *
     * @param configKey 参数键
     * @return 参数值，不存在返回 null
     */
    String getConfigValueByKey(String configKey);
}
