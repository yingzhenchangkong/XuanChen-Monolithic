package com.xuanchen.system.sysaccountrole.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.system.sysaccountrole.entity.SysAccountRole;
import com.xuanchen.system.sysaccountrole.service.ISysAccountRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

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
@RequestMapping("/system/accountrole")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class SysAccountRoleController {
    private final ISysAccountRoleService sysAccountRoleService;

    /**
     * 某角色已授权用户
     *
     * @param sysAccountRole
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/listAuthUser")
    public Result<IPage<SysAccountRole>> listAuthUser(SysAccountRole sysAccountRole,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<SysAccountRole> page = new Page<>(pageNo, pageSize);
        IPage<SysAccountRole> pageList = sysAccountRoleService.listAuthUser(page, sysAccountRole.getRoleId());
        return Result.success(pageList);
    }

    /**
     * 某角色未授权用户
     *
     * @param sysAccountRole
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/listUnAuthUser")
    public Result<IPage<SysAccountRole>> listUnAuthUser(SysAccountRole sysAccountRole,
                                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<SysAccountRole> page = new Page<>(pageNo, pageSize);
        IPage<SysAccountRole> pageList = sysAccountRoleService.listUnAuthUser(page, sysAccountRole.getRoleId());
        return Result.success(pageList);
    }

    /**
     * 为某角色分配用户
     *
     * @param sysAccountRole
     * @return
     */
    @PostMapping("/auth")
    public Result<String> auth(@RequestBody SysAccountRole sysAccountRole) {
        sysAccountRoleService.save(sysAccountRole);
        return Result.success("授权成功!");
    }

    /**
     * 为某角色批量分配用户
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/authBatch")
    public Result<String> authBatch(@RequestBody JsonNode jsonObject) {
        JsonNode jsonArray = jsonObject.get("accountIds");
        String roleId = jsonObject.get("roleId").asText();
        List<SysAccountRole> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            SysAccountRole sysAccountRole = new SysAccountRole();
            sysAccountRole.setAccountId(jsonArray.get(i).asText());
            sysAccountRole.setRoleId(roleId);
            list.add(sysAccountRole);
        }
        sysAccountRoleService.saveBatch(list);
        return Result.success("批量授权成功!");
    }

    /**
     * 为某角色取消分配用户
     *
     * @param sysAccountRole
     * @return
     */
    @PostMapping("/cancelAuth")
    public Result<String> cancelAuth(@RequestBody SysAccountRole sysAccountRole) {
        QueryWrapper<SysAccountRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account_id", sysAccountRole.getAccountId())
                .eq("role_id", sysAccountRole.getRoleId());
        sysAccountRoleService.remove(queryWrapper);
        return Result.success("取消授权成功!");
    }

    /**
     * 为某角色批量取消分配用户
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/cancelAuthBatch")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> cancelAuthBatch(@RequestBody JsonNode jsonObject) {
        JsonNode jsonArray = jsonObject.get("accountIds");
        String roleId = jsonObject.get("roleId").asText();
        Map<String, Object> columnMap = new HashMap<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            columnMap.put("account_id", jsonArray.get(i).asText());
            columnMap.put("role_id", roleId);
            sysAccountRoleService.removeByMap(columnMap);
        }
        return Result.success("批量取消授权成功!");
    }
}
