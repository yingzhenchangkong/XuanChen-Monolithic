package com.xuanchen.monitor.onlineuser.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;
import com.xuanchen.monitor.onlineuser.mapper.OnlineUserInfoMapper;
import com.xuanchen.monitor.onlineuser.service.IOnlineUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service接口实现类-->在线用户
 *
 * @author XuanChen
 * @date 2026-01-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserInfoServiceImpl extends ServiceImpl<OnlineUserInfoMapper, OnlineUserInfo> implements IOnlineUserInfoService {
    private final RedisUtil redisUtil;
    private final IAuthServiceCommon userService;

    @Override
    public IPage<OnlineUserInfo> list(Page<OnlineUserInfo> page) {
        // 获取所有用户令牌 key（SCAN 游标扫描，禁止 KEYS 阻塞 Redis 主线程）
        Set<String> keys = redisUtil.scanKeys(AuthConst.PREFIX_USER_TOKEN + "*");
        List<OnlineUserInfo> listOnlineUser = new ArrayList<>();
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                // 单个会话键异常（脏键、用户已删除、token 无法解析等）只跳过该条，绝不能让整页列表 500
                try {
                    // 从键中提取令牌
                    String token = key.substring(AuthConst.PREFIX_USER_TOKEN.length());
                    // 防御空 token 脏键（形如 PREFIX_USER_TOKEN_），无法定位用户也无法强制退出
                    if (StringUtil.isEmpty(token)) {
                        log.warn("在线用户扫描发现空 token 脏键，已跳过：{}", key);
                        continue;
                    }
                    // 从 Redis 值中读取 token 元数据（JSON 格式）
                    Object metaObj = redisUtil.get(key);
                    String ip = "-";
                    String deviceType = "-";
                    String usernameFromMeta = null;
                    if (metaObj instanceof String) {
                        String metaStr = (String) metaObj;
                        if (metaStr.startsWith("{")) {
                            try {
                                JSONObject meta = JSON.parseObject(metaStr);
                                usernameFromMeta = meta.getString("username");
                                ip = meta.getString("ip");
                                deviceType = meta.getString("deviceType");
                            } catch (Exception ignored) {
                                // 旧结构：值本身就是 token 字符串
                            }
                        }
                    }
                    // 优先从元数据中取用户名，否则从 token 中解析
                    String username = StringUtil.isNotEmpty(usernameFromMeta)
                            ? usernameFromMeta
                            : userService.getUserNameByToken(token);
                    if (username != null) {
                        Map<String, Object> user = userService.getUserByUserName(username);
                        if (user != null) {
                            OnlineUserInfo userInfo = new OnlineUserInfo();
                            userInfo.setId(Objects.toString(user.get("id"), ""));
                            userInfo.setUserName(Objects.toString(user.get("userName"), ""));
                            userInfo.setNickName(Objects.toString(user.get("nickName"), ""));
                            userInfo.setMobile(Objects.toString(user.get("mobile"), ""));
                            userInfo.setEmail(Objects.toString(user.get("email"), ""));
                            userInfo.setAvatar(Objects.toString(user.get("avatar"), ""));
                            userInfo.setToken(token);
                            userInfo.setIp(ip);
                            userInfo.setDeviceType(deviceType);
                            listOnlineUser.add(userInfo);
                        } else {
                            // 用户已删除但会话仍在 TTL 内：清理残留会话键，避免持续出现在扫描结果中
                            redisUtil.del(key);
                            log.warn("用户 {} 已不存在，已清理其残留会话键", username);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析在线用户会话键失败，已跳过：{}，原因：{}", key, e.getMessage());
                }
            }
        }
        // 会话数据来自 Redis，无数据库分页：按 token 稳定排序后在内存中分页，保证多页数据不重不漏
        listOnlineUser.sort(java.util.Comparator.comparing(OnlineUserInfo::getToken));
        int total = listOnlineUser.size();
        page.setTotal(total);
        int pageNo = Math.max(1, (int) page.getCurrent());
        int pageSize = Math.max(1, (int) page.getSize());
        int fromIndex = Math.min(total, (pageNo - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        return page.setRecords(listOnlineUser.subList(fromIndex, toIndex));
    }
}
