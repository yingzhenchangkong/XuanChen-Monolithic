package com.xuanchen.system.sysuserpost.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.system.sysuserpost.entity.SysUserPost;
import com.xuanchen.system.sysuserpost.service.ISysUserPostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@RestController
@RequestMapping("/system/userpost")
public class SysUserPostController {
    @Autowired
    private ISysUserPostService sysUserPostService;

    /**
     * 某岗位已分配用户
     *
     * @param sysUserPost
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listAssignUser")
    public Result listAuthUser(SysUserPost sysUserPost,
                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                               HttpServletRequest req) {
        Page<SysUserPost> page = new Page<>(pageNo, pageSize);
        IPage<SysUserPost> pageList = sysUserPostService.listAssignUser(page, sysUserPost.getPostId());
        return Result.success(pageList);
    }

    /**
     * 某岗位未分配用户
     *
     * @param sysUserPost
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listUnAssignUser")
    public Result listUnAuthUser(SysUserPost sysUserPost,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<SysUserPost> page = new Page<>(pageNo, pageSize);
        IPage<SysUserPost> pageList = sysUserPostService.listUnAssignUser(page, sysUserPost.getPostId());
        return Result.success(pageList);
    }

    /**
     * 为某岗位分配员工
     *
     * @param sysUserPost
     * @return
     */
    @PostMapping("/assign")
    public Result auth(@RequestBody SysUserPost sysUserPost) {
        sysUserPostService.save(sysUserPost);
        return Result.success("分配成功!");
    }

    /**
     * 为某岗位批量分配员工
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/assignBatch")
    public Result authBatch(@RequestBody JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.getJSONArray("userIds");
        String postId = (String) jsonObject.get("postId");
        List<SysUserPost> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            SysUserPost sysUserPost = new SysUserPost();
            sysUserPost.setUserId(jsonArray.getString(i));
            sysUserPost.setPostId(postId);
            list.add(sysUserPost);
        }
        sysUserPostService.saveBatch(list);
        return Result.success("批量分配成功!");
    }

    /**
     * 为某岗位取消分配员工
     *
     * @param sysUserPost
     * @return
     */
    @PostMapping("/cancelAssign")
    public Result cancelAuth(@RequestBody SysUserPost sysUserPost) {
        QueryWrapper<SysUserPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", sysUserPost.getUserId())
                .eq("post_id", sysUserPost.getPostId());
        sysUserPostService.remove(queryWrapper);
        return Result.success("取消分配成功!");
    }

    /**
     * 为某岗位批量取消分配员工
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/cancelAssignBatch")
    public Result cancelAuthBatch(@RequestBody JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.getJSONArray("userIds");
        String roleId = (String) jsonObject.get("postId");
        Map<String, Object> columnMap = new HashMap<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            columnMap.put("user_id", jsonArray.getString(i));
            columnMap.put("post_id", roleId);
            sysUserPostService.removeByMap(columnMap);
        }
        return Result.success("批量取消分配成功!");
    }
}
