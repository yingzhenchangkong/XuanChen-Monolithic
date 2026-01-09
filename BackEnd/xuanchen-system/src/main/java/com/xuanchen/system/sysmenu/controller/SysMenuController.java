package com.xuanchen.system.sysmenu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysmenu.entity.SysMenu;
import com.xuanchen.system.sysmenu.entity.SysMenuTree;
import com.xuanchen.system.sysmenu.service.ISysMenuService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->菜单
 *
 * @author XuanChen
 * @date 2025-03-31
 */
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {
    @Autowired
    private ISysMenuService sysMenuService;

    /**
     * 树形列表 无分页
     *
     * @param sysMenu
     * @return
     */
    @GetMapping("/list")
    public Result list(SysMenu sysMenu) {
        QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("order_no");
        List<SysMenu> list = sysMenuService.list(queryWrapper);
        List<SysMenuTree> listTree = new ArrayList<>();
        sysMenuService.listToTree(listTree, list, null);
        Map<String, Object> map = new HashMap<>();
        map.put("records", listTree);
        return Result.success(map);
    }

    /**
     * 菜单
     *
     * @return
     */
    @RequestMapping("/authList")
    public Result authList(HttpServletRequest request) {
        String token = request.getHeader(AuthConst.XC_ACCESS_TOKEN);
        String userName = JwtUtil.getUsername(token);
        List<SysMenu> listMenu = sysMenuService.listMenuByUserName(userName);
        List<SysMenuTree> listMenuTree = new ArrayList<>();
        sysMenuService.listToTree(listMenuTree, listMenu, null);
        return Result.success(listMenuTree);
    }

    /**
     * 添加
     *
     * @param sysMenu
     * @return
     */
    @PostMapping(value = "/add")
    public Result add(@RequestBody SysMenu sysMenu) {
        if (sysMenu.getParentId() != null) {
            SysMenu sysMenuP = sysMenuService.getById(sysMenu.getParentId());
            if (sysMenuP.getIsLeaf() == true) {
                sysMenuP.setIsLeaf(false);
                sysMenuService.updateById(sysMenuP);
            }
        }
        sysMenu.setIsLeaf(true);
        sysMenuService.save(sysMenu);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysMenu
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody SysMenu sysMenu) {
        if (sysMenu.getParentId() != null) {
            SysMenu sysMenuP = sysMenuService.getById(sysMenu.getParentId());
            if (sysMenuP.getIsLeaf() == true) {
                sysMenuP.setIsLeaf(false);
                sysMenuService.updateById(sysMenuP);
            }
        }
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysMenu::getParentId, sysMenu.getId());
        List<SysMenu> listSysPermission = sysMenuService.list(queryWrapper);
        if (listSysPermission.size() > 0) {
            sysMenu.setIsLeaf(false);
        } else {
            sysMenu.setIsLeaf(true);
        }
        sysMenuService.updateById(sysMenu);
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result delete(@RequestParam(name = "id", required = true) String id) {
        SysMenu sysMenu = sysMenuService.getById(id);
        String parentId = sysMenu.getParentId();
        sysMenuService.removeById(id);
        if (parentId != null && parentId.length() > 0) {
            QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", parentId);
            List<SysMenu> list = sysMenuService.list(queryWrapper);
            if (list.size() == 0) {
                SysMenu sysMenuP = sysMenuService.getById(parentId);
                sysMenuP.setIsLeaf(true);
                sysMenuService.updateById(sysMenuP);
            }
        }
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 检验路由名称是否已存在
     *
     * @param sysMenu
     * @return
     */
    @GetMapping("/validateName")
    public Result validateUserName(SysMenu sysMenu) {
        boolean tf = false;
        if (StringUtil.isEmpty(sysMenu.getId())) {
            LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysMenu::getName, sysMenu.getName());
            if (sysMenuService.exists(queryWrapper)) {
                tf = true;
            }
        } else {
            if (sysMenuService.validate(sysMenu)) {
                tf = true;
            }
        }
        if (tf) {
            return Result.error("路由名称已存在！");
        } else {
            return Result.success("路由名称可用！");
        }
    }

    /**
     * 校验路由地址是否已存在
     *
     * @param sysMenu
     * @return
     */
    @GetMapping("/validatePath")
    public Result validateMobile(SysMenu sysMenu) {
        boolean tf = false;
        if (StringUtil.isEmpty(sysMenu.getId())) {
            LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysMenu::getPath, sysMenu.getPath());
            if (sysMenuService.exists(queryWrapper)) {
                tf = true;
            }
        } else {
            if (sysMenuService.validate(sysMenu)) {
                tf = true;
            }
        }
        if (tf) {
            return Result.error("路由地址已存在！");
        }
        return Result.success("路由地址可用！");
    }
}
