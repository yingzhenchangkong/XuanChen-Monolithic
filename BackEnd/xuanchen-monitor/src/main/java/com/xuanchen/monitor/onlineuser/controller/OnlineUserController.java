package com.xuanchen.monitor.onlineuser.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;
import com.xuanchen.monitor.onlineuser.service.IOnlineUserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OnlineUserController {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IOnlineUserInfoService onlineUserInfoService;

    /**
     * 获取在线用户列表
     *
     * @return 在线用户列表
     */
    @GetMapping("/list")
    public Result getOnlineUserList(OnlineUserInfo onlineUserInfo,
                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                    HttpServletRequest req) {
        Page<OnlineUserInfo> page = new Page<>(pageNo, pageSize);
        IPage<OnlineUserInfo> pageList = onlineUserInfoService.list(page);
        return Result.success(pageList);
    }

    /**
     * 强制退出用户
     *
     * @param params
     * @return 操作结果
     */
    @PostMapping("/forceLogout")
    public Result forceLogout(@RequestBody Map<String, String> params) {
        String token = params.get("token");
        if (token == null || token.isEmpty()) {
            return Result.error("令牌不能为空");
        }
        // 从Redis中删除令牌
        String key = AuthConst.PREFIX_USER_TOKEN + token;
        redisUtil.del(key);
        return Result.success("强制退出成功");
    }
}
