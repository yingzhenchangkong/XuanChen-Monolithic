package com.xuanchen.system.sysrolemenu.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysmenu.entity.SysMenu;
import com.xuanchen.system.sysmenu.service.ISysMenuService;
import com.xuanchen.system.sysrolemenu.entity.SysRoleMenu;
import com.xuanchen.system.sysrolemenu.service.ISysRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制器-->角色 菜单 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@RestController
@RequestMapping("/system/rolemenu")
public class SysRoleMenuController {
    @Autowired
    private ISysRoleMenuService sysRoleMenuService;

    @Autowired
    private ISysMenuService sysMenuService;

    /**
     * 某角色已授权菜单
     *
     * @param roleId 角色编码
     * @return
     */
    @GetMapping("/listAuthMenu")
    public Result listAuthMenu(@RequestParam(name = "roleId") String roleId) {
        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> list = sysRoleMenuService.list(queryWrapper);
        List<String> listStr = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String menuId = list.get(i).getMenuId();
            SysMenu sysMenu = sysMenuService.getById(menuId);
            if (sysMenu != null && StringUtil.isNotEmpty(sysMenu.getComponent())) {
                listStr.add(menuId);
            }
        }
        return Result.success(listStr);
    }

    /**
     * 保存授权菜单
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/saveAuthMenu")
    public Result saveAuthMenu(@RequestBody JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.getJSONArray("menuIds");
        String roleId = (String) jsonObject.get("roleId");

        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        sysRoleMenuService.remove(queryWrapper);

        List<SysRoleMenu> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            SysRoleMenu sysRoleMenu = new SysRoleMenu();
            SysMenu sysMenu = sysMenuService.getById(jsonArray.getString(i));
            if (sysMenu.getParentId() != null) {
                Boolean exists = list.stream().anyMatch(sysRoleMenuTemp ->
                        sysRoleMenuTemp.getMenuId().equals(sysMenu.getParentId()) && sysRoleMenuTemp.getRoleId().equals(roleId)
                );
                if (!exists) {
                    SysRoleMenu sysRoleMenuParent = new SysRoleMenu();
                    sysRoleMenuParent.setMenuId(sysMenu.getParentId());
                    sysRoleMenuParent.setRoleId(roleId);
                    list.add(sysRoleMenuParent);
                }
            }
            sysRoleMenu.setMenuId(jsonArray.getString(i));
            sysRoleMenu.setRoleId(roleId);
            list.add(sysRoleMenu);
        }
        sysRoleMenuService.saveBatch(list);
        return Result.success("保存成功!");
    }
}
