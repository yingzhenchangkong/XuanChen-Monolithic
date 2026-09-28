package com.xuanchen.monitor.onlineuser.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;
import com.xuanchen.monitor.onlineuser.service.IOnlineUserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 控制器-->在线用户
 *
 * @author XuanChen
 * @date 2026-01-28
 */
@RestController
@RequestMapping("/monitor/onlineUser")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class OnlineUserController {
    private final RedisUtil redisUtil;
    private final IOnlineUserInfoService onlineUserInfoService;

    @GetMapping("/list")
    public Result<IPage<OnlineUserInfo>> getOnlineUserList(OnlineUserInfo onlineUserInfo,
                                                           @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<OnlineUserInfo> page = new Page<>(pageNo, pageSize);
        IPage<OnlineUserInfo> pageList = onlineUserInfoService.list(page);
        return Result.success(pageList);
    }

    /**
     * 强制退出用户：清理 token 元数据 + IP 索引
     */
    @PostMapping("/forceLogout")
    public Result<String> forceLogout(@RequestBody Map<String, String> params) {
        String token = params.get("token");
        if (token == null || token.isEmpty()) {
            return Result.badRequest("令牌不能为空");
        }
        String tokenKey = AuthConst.PREFIX_USER_TOKEN + token;
        Object metaObj = redisUtil.get(tokenKey);
        if (metaObj == null) {
            // 兜底：token 可能已过期，直接按 key 删除
            redisUtil.del(tokenKey);
            return Result.success("会话已过期，已清理");
        }
        // 清理 token 元数据
        redisUtil.del(tokenKey);

        // 清理 用户名-IP 索引
        if (metaObj instanceof String && ((String) metaObj).startsWith("{")) {
            try {
                JSONObject meta = JSON.parseObject((String) metaObj);
                String username = meta.getString("username");
                String ip = meta.getString("ip");
                if (StringUtil.isNotEmpty(username) && StringUtil.isNotEmpty(ip)) {
                    redisUtil.del(AuthConst.PREFIX_USER_BY_IP + username + "_" + ip);
                }
            } catch (Exception ignored) {
                // JSON 解析失败，不影响返回
            }
        }
        return Result.success("强制退出成功");
    }
}
