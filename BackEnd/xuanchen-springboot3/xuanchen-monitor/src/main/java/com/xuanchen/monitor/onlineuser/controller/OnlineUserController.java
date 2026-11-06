package com.xuanchen.monitor.onlineuser.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.server.WebSocketServer;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;
import com.xuanchen.monitor.onlineuser.service.IOnlineUserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.annotation.RequiresRoles;
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
@RequiresRoles("admin")
@RequestMapping("/monitor/onlineUser")
public class OnlineUserController {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IOnlineUserInfoService onlineUserInfoService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 获取在线用户列表
     *
     * @return 在线用户列表
     */
    @GetMapping("/list")
    public Result<IPage<OnlineUserInfo>> getOnlineUserList(OnlineUserInfo onlineUserInfo,
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
    public Result<String> forceLogout(@RequestBody Map<String, String> params) {
        String token = params.get("token");
        if (token == null || token.isEmpty()) {
            return Result.error("令牌不能为空");
        }
        // 解析在线会话元数据（用户名、来源 IP、会话ID）；元数据缺失/损坏时降级为从 token 解析用户名，
        // 保证"刷新令牌撤销"不会被元数据问题绕过
        Object metaObj = redisUtil.get(AuthConst.PREFIX_ONLINE_META + token);
        String userName = null;
        String loginIp = null;
        String sessionId = null;
        if (metaObj instanceof String metaStr && metaStr.startsWith("{")) {
            try {
                Map<String, Object> meta = JsonUtils.parseToMap(metaStr);
                userName = (String) meta.get("userName");
                loginIp = (String) meta.get("ip");
                sessionId = meta.get("sessionId") == null ? null : meta.get("sessionId").toString();
            } catch (Exception ignored) {
                //元数据损坏不影响强制退出主流程，下方从 token 兜底解析用户名
            }
        }
        if (StringUtil.isEmpty(userName)) {
            userName = authServiceCommon.getUserNameByToken(token);
        }

        // 1、从Redis中删除令牌及在线会话元数据、同IP会话索引
        redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
        redisUtil.del(AuthConst.PREFIX_ONLINE_META + token);
        if (StringUtil.isNotEmpty(userName) && StringUtil.isNotEmpty(loginIp)) {
            redisUtil.del(AuthConst.PREFIX_USER_BY_IP + userName + "_" + loginIp);
        }

        // 2、撤销被踢会话的"记住我"刷新令牌。只删访问令牌不改这一步时，被踢客户端仍持有
        //    httpOnly 刷新 Cookie，下一次请求 401 会静默调用 /auth/refresh 换发新访问令牌，
        //    前端表现为"点了强制退出却没有退出"（勾选"记住我"时必现，未勾选时无刷新令牌故正常）。
        //    刷新令牌按 sessionId 与会话/终端绑定，因此这里只撤销本条会话的刷新令牌，
        //    同账号在其他终端的"记住我"会话不受影响。
        if (StringUtil.isNotEmpty(userName)) {
            if (StringUtil.isNotEmpty(sessionId)) {
                authServiceCommon.revokeRefreshTokensBySession(userName, sessionId);
            } else {
                // 兜底：会话元数据缺失/为历史数据（无 sessionId）时无法精确定位，
                // 按账号级撤销，宁可多踢也不能让被踢端凭旧刷新 Cookie 复活
                authServiceCommon.revokeRefreshTokens(userName);
            }
        }

        // 3、断开被踢会话的 WebSocket 长连接。会话已失效，长连接不能再存活：
        //    否则它仍能收到服务端推送（消息泄露给已失效会话），且"强制退出"要等到
        //    被踢端下一次 HTTP 请求 401 才生效。以 1008 关闭可让被踢端立即停止重连
        //    并跳转登录页，把"软踢"变成即时可见的踢出。
        WebSocketServer.closeSessionOnKick(sessionId, token, "您已被管理员强制退出");

        return Result.success("强制退出成功");
    }
}
