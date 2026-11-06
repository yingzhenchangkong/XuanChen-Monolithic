package com.xuanchen.system.sysrolemenu.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysmenu.entity.SysMenu;
import com.xuanchen.system.sysmenu.service.ISysMenuService;
import com.xuanchen.system.sysrolemenu.entity.SysRoleMenu;
import com.xuanchen.system.sysrolemenu.service.ISysRoleMenuService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequiresRoles("admin")
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
            if (sysMenu == null) {
                continue;
            }
            // 回显条件 = "组件非空" 或 "叶子节点"：
            // 1) 组件非空：普通页面菜单（含带子按钮的 C 菜单）；
            // 2) 叶子节点：组件为空但确为叶子页面的菜单（如"待建"占位页、外链），
            //    这类菜单保存成功、路由菜单栏也显示，但按旧条件会被过滤导致重新打开时不显示勾选；
            //    按钮类型（F，叶子且无组件）同样借此正确回显。
            // 3) 排除目录（isLeaf=0 且无组件）：目录进入 checkedKeys 会被树控件向下级联
            //    勾选全部子节点，重新打开即"凭空"勾上未授权的子菜单。
            // isLeaf 由菜单新增/编辑维护（有子节点回写 0，无子节点回写 1），可作可靠判据。
            boolean isLeaf = Integer.valueOf(1).equals(sysMenu.getIsLeaf());
            if (isLeaf || StringUtil.isNotEmpty(sysMenu.getComponent())) {
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
    public Result<String> saveAuthMenu(@RequestBody JsonNode jsonObject) {
        JsonNode jsonArray = jsonObject.get("menuIds");
        String roleId = jsonObject.get("roleId").asText();
        if (StringUtil.isEmpty(roleId)) {
            return Result.badRequest("角色ID不能为空！");
        }
        if (jsonArray == null || jsonArray.isEmpty()) {
            return Result.badRequest("菜单ID列表不能为空！");
        }

        // 去重、去空白（StringUtil.isNotEmpty 不识别纯空白，必须再判 trim）
        Set<String> requestMenuIds = new HashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            String menuId = jsonArray.get(i).asText();
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
