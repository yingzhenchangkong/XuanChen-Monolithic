package com.xuanchen.system.sysmenu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysmenu.entity.SysMenu;
import com.xuanchen.system.sysmenu.entity.SysMenuTree;
import com.xuanchen.system.sysmenu.service.ISysMenuService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 树形列表 无分页
     *
     * @param sysMenu
     * @return
     */
    @GetMapping("/list")
    @RequiresRoles("admin")
    public Result<Map<String, Object>> list(SysMenu sysMenu) {
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
     * 添加
     *
     * @param sysMenu
     * @return
     */
    @PostMapping(value = "/add")
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> add(@RequestBody SysMenu sysMenu) {
        if (StringUtil.isNotEmpty(sysMenu.getParentId())) {
            SysMenu sysMenuP = sysMenuService.getById(sysMenu.getParentId());
            if (sysMenuP == null) {
                // 指定的上级已被删除：拒绝落库，避免产生孤儿菜单
                return Result.error("上级菜单不存在，请刷新后重试！");
            }
            // isLeaf 为可空 Integer（1是 0否），禁止直接拆箱（null 会 NPE）。
            // 只要当前不是明确的"非叶子(0)"（即 1/null），挂上子节点后都回写为 0，顺带修复历史脏值
            if (!Integer.valueOf(0).equals(sysMenuP.getIsLeaf())) {
                sysMenuP.setIsLeaf(0);
                sysMenuService.updateById(sysMenuP);
            }
        }
        sysMenu.setIsLeaf(1);
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
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> edit(@RequestBody SysMenu sysMenu) {
        if (StringUtil.isEmpty(sysMenu.getId())) {
            return Result.badRequest("缺少菜单ID！");
        }
        // 记录变更前的旧上级：改挂后需回刷其叶子标记（原上级可能因此变成无子节点）
        SysMenu oldMenu = sysMenuService.getById(sysMenu.getId());
        if (oldMenu == null) {
            return Result.badRequest("菜单不存在或已被删除！");
        }
        if (StringUtil.isNotEmpty(sysMenu.getParentId())) {
            // 不允许把自身设为上级，否则形成环导致菜单树无法构建
            if (sysMenu.getParentId().equals(sysMenu.getId())) {
                return Result.error("上级菜单不能选择自身！");
            }
            SysMenu sysMenuP = sysMenuService.getById(sysMenu.getParentId());
            if (sysMenuP == null) {
                return Result.error("上级菜单不存在，请刷新后重试！");
            }
            if (!Integer.valueOf(0).equals(sysMenuP.getIsLeaf())) {
                sysMenuP.setIsLeaf(0);
                sysMenuService.updateById(sysMenuP);
            }
        }
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysMenu::getParentId, sysMenu.getId());
        long childCount = sysMenuService.count(queryWrapper);
        // 按是否仍有子节点回写叶子标记，避免标记值与实际层级漂移
        sysMenu.setIsLeaf(childCount == 0 ? 1 : 0);
        // 🔴 MP updateById 忽略 null 字段：前端清空上级菜单（allow-clear 后 parentId 为 undefined，
        // JSON 序列化时整个字段被丢弃，后端反序列化为 null），必须用 UpdateWrapper 显式把
        // parent_id 置 NULL，否则该列保持旧值——表现为"取消上级菜单保存后仍是之前选中的上级"。
        // 实体与 wrapper 同时携带 parent_id 会产生重复 SET 列，先从实体上摘除、统一由 wrapper 写
        String newParentId = sysMenu.getParentId();
        sysMenu.setParentId(null);
        LambdaUpdateWrapper<SysMenu> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysMenu::getId, sysMenu.getId())
                .set(SysMenu::getParentId, newParentId);
        if (!sysMenuService.update(sysMenu, updateWrapper)) {
            return Result.badRequest("菜单不存在或已被删除！");
        }
        // 上级发生变更时回刷原上级的叶子标记（新上级已在校验分支中回写为非叶子）
        if (StringUtil.isNotEmpty(oldMenu.getParentId())
                && !oldMenu.getParentId().equals(newParentId)) {
            LambdaQueryWrapper<SysMenu> oldParentChildWrapper = new LambdaQueryWrapper<>();
            oldParentChildWrapper.eq(SysMenu::getParentId, oldMenu.getParentId());
            if (sysMenuService.count(oldParentChildWrapper) == 0) {
                SysMenu oldParent = new SysMenu();
                oldParent.setId(oldMenu.getParentId());
                oldParent.setIsLeaf(1);
                sysMenuService.updateById(oldParent);
            }
        }
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        if (StringUtil.isEmpty(id)) {
            return Result.error("菜单ID不能为空！");
        }
        SysMenu sysMenu = sysMenuService.getById(id);
        if (sysMenu == null) {
            // 并发删除/非法ID：幂等返回成功，避免 getById 为 null 后继续拆 parentId 触发 NPE
            return Result.success(TipConst.DEL_SUCC);
        }
        // 删除前强制校验子节点：直接删除父节点会让子节点 parent_id 悬挂，菜单树与权限分配出现孤儿数据
        long childCount = sysMenuService.count(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            return Result.error("该菜单存在 " + childCount + " 个子菜单，请先删除全部子菜单后再删除！");
        }
        String parentId = sysMenu.getParentId();
        sysMenuService.removeById(id);
        if (StringUtil.isNotEmpty(parentId)) {
            QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", parentId);
            List<SysMenu> list = sysMenuService.list(queryWrapper);
            if (list.isEmpty()) {
                // 最后一个子节点被删：父级回归叶子；父级可能已被并发删除，二次判空
                SysMenu sysMenuP = sysMenuService.getById(parentId);
                if (sysMenuP != null) {
                    sysMenuP.setIsLeaf(1);
                    sysMenuService.updateById(sysMenuP);
                }
            }
        }
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 菜单
     *
     * @return
     */
    @RequestMapping("/authList")
    public Result<List<SysMenuTree>> authList(HttpServletRequest request) {
        String token = request.getHeader(AuthConst.XC_ACCESS_TOKEN);
        String userName = authServiceCommon.getUserNameByToken(token);
        List<SysMenu> listMenu = sysMenuService.listMenuByUserName(userName);
        List<SysMenuTree> listMenuTree = new ArrayList<>();
        sysMenuService.listToTree(listMenuTree, listMenu, null);
        return Result.success(listMenuTree);
    }

    /**
     * 状态修改
     *
     * @param sysMenu
     * @param request
     * @return
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRoles("admin")
    public Result<String> changeStatus(@RequestBody SysMenu sysMenu, HttpServletRequest request) {
        if (StringUtil.isEmpty(sysMenu.getId())) {
            return Result.badRequest("缺少菜单ID！");
        }
        UpdateWrapper<SysMenu> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", sysMenu.getStatus()).eq("id", sysMenu.getId());
        // 影响行数为 0（id 不存在或已被删除）必须返回失败，避免前端误判成功
        if (!sysMenuService.update(updateWrapper)) {
            return Result.badRequest("菜单不存在或已被删除！");
        }
        return Result.success("状态修改成功！");
    }

    /**
     * 校验 参数 是否已存在
     *
     * @param sysMenu
     * @return
     */
    @GetMapping("/validate")
    @RequiresRoles("admin")
    public Result<String> validate(SysMenu sysMenu) {
        boolean exists = StringUtil.isEmpty(sysMenu.getId())
                ? sysMenuService.ifExistsNoId(sysMenu)
                : sysMenuService.ifExistsId(sysMenu);
        return exists
                ? Result.conflict(TipConst.PARAM_EXISTS)
                : Result.success(TipConst.PARAM_AVAILABLE);
    }
}
