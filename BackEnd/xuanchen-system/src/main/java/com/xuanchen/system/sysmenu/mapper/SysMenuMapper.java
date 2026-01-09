package com.xuanchen.system.sysmenu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.system.sysmenu.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->菜单
 *
 * @author XuanChen
 * @date 2025-04-04
 */
@Repository
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    List<SysMenu> listMenuByUserName(String userName);

    List<String> listAuthByUserId(String userId);

    List<SysMenu> validate(@Param("sysMenu") SysMenu sysMenu);
}
