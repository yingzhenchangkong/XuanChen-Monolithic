package com.xuanchen.auth.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.auth.auth.entity.Auth;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
@Repository
public interface AuthMapper extends BaseMapper<Auth> {
    /**
     * 查询用户已启用角色的角色编码
     *
     * @param userName 用户名
     * @return 角色编码集合
     */
    List<String> listRoleCodesByUserName(@Param("userName") String userName);

    /**
     * 查询用户已启用角色关联的全部菜单权限标识（perms）
     *
     * @param userName 用户名
     * @return 权限标识集合
     */
    List<String> listPermsByUserName(@Param("userName") String userName);

    /**
     * 按 config_key 查询启用中的系统参数值
     *
     * @param configKey 参数键
     * @return 参数值，不存在返回 null
     */
    String getConfigValueByKey(@Param("configKey") String configKey);

    /**
     * 查询登录所需账号信息（身份与凭证均在 sys_account）
     *
     * @param userName 用户名
     * @return 包含账号身份与凭证字段的 Map
     */
    java.util.Map<String, Object> getUserWithProfile(@Param("userName") String userName);

    /**
     * 检查邮箱是否已被其他账号绑定（sys_account.email）
     *
     * @param email 邮箱
     * @return 匹配条数
     */
    long countAccountByEmail(@Param("email") String email);
}
