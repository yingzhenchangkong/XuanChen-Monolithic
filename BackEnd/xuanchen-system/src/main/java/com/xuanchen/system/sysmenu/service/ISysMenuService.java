package com.xuanchen.system.sysmenu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.system.sysmenu.entity.SysMenu;
import com.xuanchen.system.sysmenu.entity.SysMenuTree;

import java.util.List;

/**
 * Service接口-->菜单
 *
 * @author XuanChen
 * @date 2025-04-04
 */
public interface ISysMenuService extends IService<SysMenu> {
    List<SysMenu> listMenuByUserName(String userName);

    List<String> listAuthByUserId(String userId);

    void listToTree(List<SysMenuTree> listTree, List<SysMenu> list, SysMenuTree temp);

    Boolean validate(SysMenu sysMenu);
}
