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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 控制器-->角色 菜单 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@RestController
@RequestMapping("/system/rolemenu")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class SysRoleMenuController {
    private final ISysRoleMenuService sysRoleMenuService;
    private final ISysMenuService sysMenuService;

    /**
     * 某角色已授权菜单
     *
     * @param roleId 角色编码
     * @return
     */
    @GetMapping("/listAuthMenu")
    public Result<List<String>> listAuthMenu(@RequestParam(name = "roleId") String roleId) {
        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> list = sysRoleMenuService.list(queryWrapper);
        if (list.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        // 批量取菜单，避免循环内逐条 getById 的 N+1 查询
        List<String> menuIds = list.stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        Map<String, SysMenu> menuMap = sysMenuService.listByIds(menuIds).stream()
                .collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> a));
        List<String> listStr = new ArrayList<>();
        for (SysRoleMenu roleMenu : list) {
            String menuId = roleMenu.getMenuId();
            SysMenu sysMenu = menuMap.get(menuId);
            if (sysMenu != null && StringUtil.isNotEmpty(sysMenu.getComponent())) {
                listStr.add(menuId);
            }
        }
        return Result.success(listStr);
    }

    /**
     * 保存授权菜单
     * 安全/正确性约束：
     * 1. 整个"先全删再批量插"必须在同一事务内，中途异常整体回滚，避免角色菜单被清空；
     * 2. 菜单只批量查询一次（消除 N+1），且 menuId 不存在直接拒绝（原实现 getById 返回 null 后 NPE）；
     * 3. 用去重 Map 组装关系，避免重复行。
     *
     * @param jsonObject roleId + menuIds
     * @return 保存结果
     */
    @PostMapping("/saveAuthMenu")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveAuthMenu(@RequestBody JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.getJSONArray("menuIds");
        String roleId = jsonObject.getString("roleId");
        if (StringUtil.isEmpty(roleId)) {
            return Result.badRequest("角色ID不能为空！");
        }
        if (jsonArray == null) {
            return Result.badRequest("菜单ID列表不能为空！");
        }

        // 去重、去空白（StringUtil.isNotEmpty 不识别纯空白，必须再判 trim）
        Set<String> requestMenuIds = new HashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            String menuId = jsonArray.getString(i);
            if (StringUtil.isNotEmpty(menuId) && !menuId.trim().isEmpty()) {
                requestMenuIds.add(menuId.trim());
            }
        }
        if (requestMenuIds.isEmpty()) {
            // 允许"清空全部授权"：删除后不插入
            LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
            sysRoleMenuService.remove(queryWrapper);
            return Result.success("保存成功!");
        }

        // 一次性查出全部菜单：id -> 菜单
        Map<String, SysMenu> menuMap = sysMenuService.listByIds(requestMenuIds).stream()
                .collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> a));
        if (menuMap.size() != requestMenuIds.size()) {
            return Result.badRequest("存在无效的菜单ID，请刷新后重试！");
        }

        // menuId -> 关系（含需要补齐的父级菜单），LinkedHashMap 保证幂等去重
        Map<String, SysRoleMenu> toSave = new LinkedHashMap<>();
        for (String menuId : requestMenuIds) {
            SysMenu sysMenu = menuMap.get(menuId);
            if (StringUtil.isNotEmpty(sysMenu.getParentId()) && !requestMenuIds.contains(sysMenu.getParentId())) {
                toSave.computeIfAbsent(sysMenu.getParentId(), pid -> buildRoleMenu(roleId, pid));
            }
            toSave.putIfAbsent(menuId, buildRoleMenu(roleId, menuId));
        }

        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        sysRoleMenuService.remove(queryWrapper);
        sysRoleMenuService.saveBatch(new ArrayList<>(toSave.values()));
        return Result.success("保存成功!");
    }

    private SysRoleMenu buildRoleMenu(String roleId, String menuId) {
        SysRoleMenu sysRoleMenu = new SysRoleMenu();
        sysRoleMenu.setMenuId(menuId);
        sysRoleMenu.setRoleId(roleId);
        return sysRoleMenu;
    }
}
