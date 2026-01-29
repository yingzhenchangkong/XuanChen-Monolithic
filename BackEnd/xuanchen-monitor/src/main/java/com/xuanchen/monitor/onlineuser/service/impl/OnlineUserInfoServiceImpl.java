package com.xuanchen.monitor.onlineuser.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.auth.utils.JwtUtil;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;
import com.xuanchen.monitor.onlineuser.mapper.OnlineUserInfoMapper;
import com.xuanchen.monitor.onlineuser.service.IOnlineUserInfoService;
import com.xuanchen.system.sysuser.entity.SysUser;
import com.xuanchen.system.sysuser.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    private ISysUserService sysUserService;

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
                String username = JwtUtil.getUsername(token);
                if (username != null) {
                    // 根据用户名获取用户信息
                    SysUser sysUser = sysUserService.getByUsername(username);
                    if (sysUser != null) {
                        OnlineUserInfo userInfo = new OnlineUserInfo();
                        userInfo.setId(sysUser.getId());
                        userInfo.setUserName(sysUser.getUserName());
                        userInfo.setNickName(sysUser.getNickName());
                        userInfo.setMobile(sysUser.getMobile());
                        userInfo.setEmail(sysUser.getEmail());
                        userInfo.setAvatar(sysUser.getAvatar());
                        userInfo.setToken(token);
                        listOnlineUser.add(userInfo);
                    }
                }
            }
        }
        return page.setRecords(listOnlineUser);
    }
}
