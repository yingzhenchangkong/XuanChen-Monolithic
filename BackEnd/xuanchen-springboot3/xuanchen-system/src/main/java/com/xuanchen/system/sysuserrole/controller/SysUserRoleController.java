package com.xuanchen.system.sysuserrole.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.system.sysuserrole.entity.SysUserRole;
import com.xuanchen.system.sysuserrole.service.ISysUserRoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->用户 角色 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@RestController
@RequestMapping("/system/userrole")
public class SysUserRoleController {
    @Autowired
    private ISysUserRoleService sysUserRoleService;

    /**
     * 某角色已授权用户
     *
     * @param sysUserRole
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listAuthUser")
    public Result listAuthUser(SysUserRole sysUserRole,
                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                               HttpServletRequest req) {
        Page<SysUserRole> page = new Page<>(pageNo, pageSize);
        IPage<SysUserRole> pageList = sysUserRoleService.listAuthUser(page, sysUserRole.getRoleId());
        return Result.success(pageList);
    }

    /**
     * 某角色未授权用户
     *
     * @param sysUserRole
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listUnAuthUser")
    public Result listUnAuthUser(SysUserRole sysUserRole,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<SysUserRole> page = new Page<>(pageNo, pageSize);
        IPage<SysUserRole> pageList = sysUserRoleService.listUnAuthUser(page, sysUserRole.getRoleId());
        return Result.success(pageList);
    }

    /**
     * 为某角色分配用户
     *
     * @param sysUserRole
     * @return
     */
    @PostMapping("/auth")
    public Result auth(@RequestBody SysUserRole sysUserRole) {
        sysUserRoleService.save(sysUserRole);
        return Result.success("授权成功!");
    }

    /**
     * 为某角色批量分配用户
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/authBatch")
    public Result authBatch(@RequestBody JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.getJSONArray("userIds");
        String roleId = (String) jsonObject.get("roleId");
        List<SysUserRole> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(jsonArray.getString(i));
            sysUserRole.setRoleId(roleId);
            list.add(sysUserRole);
        }
        sysUserRoleService.saveBatch(list);
        return Result.success("批量授权成功!");
    }

    /**
     * 为某角色取消分配用户
     *
     * @param sysUserRole
     * @return
     */
    @PostMapping("/cancelAuth")
    public Result cancelAuth(@RequestBody SysUserRole sysUserRole) {
        QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", sysUserRole.getUserId())
                .eq("role_id", sysUserRole.getRoleId());
        sysUserRoleService.remove(queryWrapper);
        return Result.success("取消授权成功!");
    }

    /**
     * 为某角色批量取消分配用户
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/cancelAuthBatch")
    public Result cancelAuthBatch(@RequestBody JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.getJSONArray("userIds");
        String roleId = (String) jsonObject.get("roleId");
        Map<String, Object> columnMap = new HashMap<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            columnMap.put("user_id", jsonArray.getString(i));
            columnMap.put("role_id", roleId);
            sysUserRoleService.removeByMap(columnMap);
        }
        return Result.success("批量取消授权成功!");
    }
}
