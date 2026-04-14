package com.xuanchen.system.sysuserdept.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.system.sysdept.entity.SysDept;
import com.xuanchen.system.sysdept.service.ISysDeptService;
import com.xuanchen.system.sysuser.entity.SysUser;
import com.xuanchen.system.sysuser.service.ISysUserService;
import com.xuanchen.system.sysuserdept.entity.SysUserDept;
import com.xuanchen.system.sysuserdept.service.ISysUserDeptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制器-->用户 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
@RestController
@RequestMapping("/system/userdept")
public class SysUserDeptController {
    @Autowired
    private ISysUserDeptService sysUserDeptService;
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private ISysDeptService sysDeptService;

    /**
     * 部门已授权用户
     *
     * @param sysUserDept
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listDeptUser")
    public Result listDeptUser(SysUserDept sysUserDept,
                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                               HttpServletRequest req) {
        Page<SysUserDept> page = new Page<>(pageNo, pageSize);
        IPage<SysUserDept> pageList = sysUserDeptService.listDeptUser(page, sysUserDept.getDeptCode());
        return Result.success(pageList);
    }

    /**
     * 为某部门关联用户
     *
     * @param sysUserDept
     * @return
     */
    @PostMapping("/link")
    public Result link(@RequestBody SysUserDept sysUserDept) {
        StringBuilder linkedUser = new StringBuilder();
        StringBuilder linkSuccUser = new StringBuilder();
        List<SysUserDept> list = new ArrayList<>();
        for (String userName : sysUserDept.getListUser()) {
            SysUserDept sysUserDeptTemp = new SysUserDept();
            QueryWrapper<SysUser> qwUser = new QueryWrapper<>();
            qwUser.eq("user_name", userName);
            SysUser sysUser = sysUserService.getOne(qwUser);
            sysUserDeptTemp.setUserId(sysUser.getId());
            QueryWrapper<SysDept> qwDept = new QueryWrapper<>();
            qwDept.eq("dept_code", sysUserDept.getDeptCode());
            SysDept sysDept = sysDeptService.getOne(qwDept);
            sysUserDeptTemp.setDeptId(sysDept.getDeptCode());
            QueryWrapper<SysUserDept> qwUserDept = new QueryWrapper<>();
            qwUserDept.eq("user_id", sysUserDeptTemp.getUserId());
            qwUserDept.eq("dept_id", sysUserDeptTemp.getDeptId());
            if (sysUserDeptService.count(qwUserDept) > 0) {
                linkedUser.append(sysUser.getNickName()).append(",");
            } else {
                linkSuccUser.append(sysUser.getNickName()).append(",");
                list.add(sysUserDeptTemp);
            }
        }
        sysUserDeptService.saveBatch(list);
        String message = "";
        if (!linkedUser.isEmpty()) {
            linkedUser = new StringBuilder(linkedUser.substring(0, linkedUser.length() - 1));
        }
        if (!linkSuccUser.isEmpty()) {
            linkSuccUser = new StringBuilder(linkSuccUser.substring(0, linkSuccUser.length() - 1));
        }
        if ((!linkedUser.isEmpty()) && (linkSuccUser.isEmpty())) {
            message = linkedUser + "已关联!无用户关联成功!";
        } else if ((linkedUser.isEmpty()) && (!linkSuccUser.isEmpty())) {
            message = linkSuccUser + "关联成功!";
        } else if (!linkedUser.isEmpty()) {
            message = linkedUser + "已关联," + linkSuccUser + "关联成功!";
        }
        return Result.success(message);
    }

    /**
     * 为某部门取消关联用户
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/unlink")
    public Result delete(@RequestParam(name = "id", required = true) String id) {
        sysUserDeptService.removeById(id);
        return Result.success("取消关联成功!");
    }
}
