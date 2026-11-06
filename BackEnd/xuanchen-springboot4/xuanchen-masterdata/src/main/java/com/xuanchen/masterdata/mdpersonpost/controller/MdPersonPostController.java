package com.xuanchen.masterdata.mdpersonpost.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.masterdata.mdpersonpost.entity.MdPersonPost;
import com.xuanchen.masterdata.mdpersonpost.service.IMdPersonPostService;
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
 * 控制器-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@RestController
@RequestMapping("/system/personpost")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class MdPersonPostController {
    private final IMdPersonPostService mdPersonPostService;

    /**
     * 某岗位已分配用户
     *
     * @param mdPersonPost
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/listAssignUser")
    public Result<IPage<MdPersonPost>> listAuthUser(MdPersonPost mdPersonPost,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<MdPersonPost> page = new Page<>(pageNo, pageSize);
        IPage<MdPersonPost> pageList = mdPersonPostService.listAssignUser(page, mdPersonPost.getPostId());
        return Result.success(pageList);
    }

    /**
     * 某岗位未分配用户
     *
     * @param mdPersonPost
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/listUnAssignUser")
    public Result<IPage<MdPersonPost>> listUnAuthUser(MdPersonPost mdPersonPost,
                                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<MdPersonPost> page = new Page<>(pageNo, pageSize);
        IPage<MdPersonPost> pageList = mdPersonPostService.listUnAssignUser(page, mdPersonPost.getPostId());
        return Result.success(pageList);
    }

    /**
     * 为某岗位分配人员
     *
     * @param mdPersonPost
     * @return
     */
    @PostMapping("/assign")
    public Result<String> auth(@RequestBody MdPersonPost mdPersonPost) {
        if (StringUtil.isEmpty(mdPersonPost.getPersonId())) {
            return Result.badRequest("缺少人员ID！");
        }
        mdPersonPostService.save(mdPersonPost);
        return Result.success("分配成功!");
    }

    /**
     * 为某岗位批量分配人员
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/assignBatch")
    public Result<String> authBatch(@RequestBody JsonNode jsonObject) {
        JsonNode jsonArray = jsonObject.get("personIds");
        String postId = jsonObject.get("postId").asText();
        List<MdPersonPost> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            MdPersonPost mdPersonPost = new MdPersonPost();
            mdPersonPost.setPersonId(jsonArray.get(i).asText());
            mdPersonPost.setPostId(postId);
            list.add(mdPersonPost);
        }
        mdPersonPostService.saveBatch(list);
        return Result.success("批量分配成功!");
    }

    /**
     * 为某岗位取消分配人员
     *
     * @param mdPersonPost
     * @return
     */
    @PostMapping("/cancelAssign")
    public Result<String> cancelAuth(@RequestBody MdPersonPost mdPersonPost) {
        QueryWrapper<MdPersonPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("person_id", mdPersonPost.getPersonId())
                .eq("post_id", mdPersonPost.getPostId());
        mdPersonPostService.remove(queryWrapper);
        return Result.success("取消分配成功!");
    }

    /**
     * 为某岗位批量取消分配人员
     *
     * @param jsonObject
     * @return
     */
    @PostMapping("/cancelAssignBatch")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> cancelAuthBatch(@RequestBody JsonNode jsonObject) {
        JsonNode jsonArray = jsonObject.get("personIds");
        String postId = jsonObject.get("postId").asText();
        Map<String, Object> columnMap = new HashMap<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            columnMap.put("person_id", jsonArray.get(i).asText());
            columnMap.put("post_id", postId);
            mdPersonPostService.removeByMap(columnMap);
        }
        return Result.success("批量取消分配成功!");
    }
}
