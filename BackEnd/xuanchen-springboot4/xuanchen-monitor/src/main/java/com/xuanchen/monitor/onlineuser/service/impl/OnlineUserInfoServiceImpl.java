package com.xuanchen.monitor.onlineuser.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.JsonUtils;
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
                                Map<String, Object> meta = JsonUtils.parseToMap(metaStr);
                                usernameFromMeta = (String) meta.get("username");
                                ip = (String) meta.get("ip");
                                deviceType = (String) meta.get("deviceType");
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
                        // 用户已不存在或账号已删除/冻结：其会话已不可能再通过认证，
                        // 不展示在在线列表中，并回收残留会话键与孤儿索引，避免脏会话残留至 TTL
                        if (user == null || isAccountInvalid(user)) {
                            evictSession(token, username);
                            log.warn("用户 {} 已删除/冻结，已清理其残留会话", username);
                        } else {
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

    /**
     * 判断会话对应用户是否已失效（软删/冻结）。
     * 仅在"明确标记为已删除或已冻结"时才回收会话；字段为 null 时视为有效——
     * 避免因 Map 键映射差异或数据异常导致批量误驱逐（含调用者自身会话），
     * 造成"刷新后被踢回登录页"的连锁故障。
     * 兼容 camelCase 与 snake_case 两种 Map 键格式。
     */
    private boolean isAccountInvalid(Map<String, Object> user) {
        Object delFlag = user.containsKey("delFlag") ? user.get("delFlag") : user.get("del_flag");
        if (delFlag instanceof Number && ((Number) delFlag).intValue() != 0) {
            return true;
        }
        Object status = user.containsKey("status") ? user.get("status") : user.get("status");
        if (status instanceof Number && ((Number) status).intValue() != 1) {
            return true;
        }
        return false;
    }

    /**
     * 回收失效会话的 Redis 键：令牌、同IP会话索引。
     * 同 IP 索引只在仍指向本令牌、无值或指向的会话本体已不存在（孤儿索引）时删除；
     * 该索引可能已被同 IP 新登录重新指向存活会话，误删会让新会话成为登录踢人扫不到的孤儿。
     */
    private void evictSession(String token, String username) {
        redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
        Set<String> indexKeys = redisUtil.scanKeys(AuthConst.PREFIX_USER_BY_IP + username + "_*");
        if (indexKeys == null || indexKeys.isEmpty()) {
            return;
        }
        for (String indexKey : indexKeys) {
            Object indexedToken = redisUtil.get(indexKey);
            if (indexedToken == null) {
                // 孤儿索引：无值
                redisUtil.del(indexKey);
                continue;
            }
            String indexed = indexedToken.toString();
            if ((token != null && token.equals(indexed))
                    || redisUtil.get(AuthConst.PREFIX_USER_TOKEN + indexed) == null) {
                // 指向本次回收的令牌，或指向的会话本体已不存在：删除；其他存活会话的索引保留
                redisUtil.del(indexKey);
            }
        }
    }
}
