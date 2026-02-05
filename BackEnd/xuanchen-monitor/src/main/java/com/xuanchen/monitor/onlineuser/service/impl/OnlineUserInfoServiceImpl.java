package com.xuanchen.monitor.onlineuser.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;
import com.xuanchen.monitor.onlineuser.mapper.OnlineUserInfoMapper;
import com.xuanchen.monitor.onlineuser.service.IOnlineUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service接口实现类-->在线用户
 *
 * @author XuanChen
 * @date 2026-01-29
 */
@Service
public class OnlineUserInfoServiceImpl extends ServiceImpl<OnlineUserInfoMapper, OnlineUserInfo> implements IOnlineUserInfoService {
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IAuthServiceCommon userService;

    @Override
    public IPage<OnlineUserInfo> list(Page<OnlineUserInfo> page) {
        // 获取所有用户令牌
        Set<String> keys = redisUtil.keys(AuthConst.PREFIX_USER_TOKEN + "*");
        List<OnlineUserInfo> listOnlineUser = new ArrayList<>();
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                // 从键中提取令牌
                String token = key.substring(AuthConst.PREFIX_USER_TOKEN.length());
                // 从令牌中提取用户名
                String username = userService.getUserNameByToken(token);
                if (username != null) {
                    // 根据用户名获取用户信息
                    Map<String, Object> user = userService.getUserByUserName(username);
                    if (user != null) {
                        OnlineUserInfo userInfo = new OnlineUserInfo();
                        userInfo.setId(user.get("id").toString());
                        userInfo.setUserName(user.get("userName").toString());
                        userInfo.setNickName(user.get("nickName").toString());
                        userInfo.setMobile(user.get("mobile").toString());
                        userInfo.setEmail(user.get("email").toString());
                        userInfo.setAvatar(user.get("avatar").toString());
                        userInfo.setToken(token);
                        listOnlineUser.add(userInfo);
                    }
                }
            }
        }
        return page.setRecords(listOnlineUser);
    }
}
