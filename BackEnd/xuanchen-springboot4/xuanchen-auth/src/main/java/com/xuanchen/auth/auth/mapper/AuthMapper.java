package com.xuanchen.auth.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.auth.auth.entity.Auth;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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
    @Select("SELECT DISTINCT r.role_code " +
            "FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE u.user_name = #{userName} " +
            "AND u.del_flag = 0 AND r.del_flag = 0 AND r.status = 1")
    List<String> listRoleCodesByUserName(@Param("userName") String userName);

    /**
     * 查询用户已启用角色关联的全部菜单权限标识（perms）
     *
     * @param userName 用户名
     * @return 权限标识集合
     */
    @Select("SELECT DISTINCT m.perms " +
            "FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "INNER JOIN sys_role_menu rm ON r.id = rm.role_id " +
            "INNER JOIN sys_menu m ON rm.menu_id = m.id " +
            "WHERE u.user_name = #{userName} " +
            "AND u.del_flag = 0 AND r.del_flag = 0 AND r.status = 1 " +
            "AND m.del_flag = 0 AND m.status = 1 " +
            "AND m.perms IS NOT NULL AND m.perms <> ''")
    List<String> listPermsByUserName(@Param("userName") String userName);

    /**
     * 按 config_key 查询启用中的系统参数值
     *
     * @param configKey 参数键
     * @return 参数值，不存在返回 null
     */
    @Select("SELECT config_value FROM sys_config " +
            "WHERE config_key = #{configKey} AND del_flag = 0 AND status = 1 LIMIT 1")
    String getConfigValueByKey(@Param("configKey") String configKey);
}
