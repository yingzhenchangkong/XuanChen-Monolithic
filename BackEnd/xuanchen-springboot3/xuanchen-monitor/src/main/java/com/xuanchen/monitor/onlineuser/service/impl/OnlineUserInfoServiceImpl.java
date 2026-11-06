package com.xuanchen.monitor.onlineuser.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.monitor.onlineuser.entity.OnlineUserInfo;
import com.xuanchen.monitor.onlineuser.mapper.OnlineUserInfoMapper;
import com.xuanchen.monitor.onlineuser.service.IOnlineUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service接口实现类-->在线用户
 *
 * @author XuanChen
 * @date 2026-01-29
 */
@Slf4j
@Service
public class OnlineUserInfoServiceImpl extends ServiceImpl<OnlineUserInfoMapper, OnlineUserInfo> implements IOnlineUserInfoService {
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IAuthServiceCommon userService;

    @Override
    public IPage<OnlineUserInfo> list(Page<OnlineUserInfo> page) {
        // 获取所有用户令牌（SCAN 游标分批迭代，禁止用 KEYS 阻塞 Redis 主线程）
        Set<String> keys = redisUtil.scanKeys(AuthConst.PREFIX_USER_TOKEN + "*");
        List<OnlineUserInfo> listOnlineUser = new ArrayList<>();
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                // 单个会话键异常（脏键、token 无法解析、元数据损坏等）只跳过该条，绝不能让整页列表 500
                try {
                    // 从键中提取令牌
                    String token = key.substring(AuthConst.PREFIX_USER_TOKEN.length());
                    // 防御空 token 脏键（形如 PREFIX_USER_TOKEN_），无法定位用户也无法强制退出
                    if (token == null || token.isEmpty()) {
                        log.warn("在线用户扫描发现空 token 脏键，已跳过：{}", key);
                        continue;
                    }
                    // 从令牌中提取用户名
                    String username = userService.getUserNameByToken(token);
                    if (username != null) {
                        // 读取登录会话元数据（登录IP、终端类型）；历史会话无元数据时展示 "-"
                        String ip = "-";
                        String deviceType = "-";
                        Object metaObj = redisUtil.get(AuthConst.PREFIX_ONLINE_META + token);
                        if (metaObj instanceof String metaStr && metaStr.startsWith("{")) {
                            try {
                                Map<String, Object> meta = JsonUtils.parseToMap(metaStr);
                                String metaIp = (String) meta.get("ip");
                                String metaDevice = (String) meta.get("deviceType");
                                if (metaIp != null && !metaIp.isEmpty()) {
                                    ip = metaIp;
                                }
                                if (metaDevice != null && !metaDevice.isEmpty()) {
                                    deviceType = metaDevice;
                                }
                            } catch (Exception ignored) {
                                //元数据损坏时按缺省展示，不影响列表
                            }
                        }
                        // 根据用户名获取用户信息
                        Map<String, Object> user = userService.getUserByUserName(username);
                        //用户已被物理删除，或账号已被删除/冻结：其 Redis 会话已不可能再通过认证，
                        //不展示在在线列表中，并顺手回收残留的令牌/元数据/同IP索引（与强制退出同一路径），
                        //避免脏会话残留至 TTL 且每次扫描都产生无效查库
                        if (user == null || isAccountInvalid(user)) {
                            evictSession(token, username);
                            continue;
                        }
                        OnlineUserInfo userInfo = new OnlineUserInfo();
                        //以下字段在库中均允许为 NULL，Map 取值直接强转，禁止对 null 调 toString
                        userInfo.setId((String) user.get("id"));
                        userInfo.setUserName((String) user.get("userName"));
                        userInfo.setNickName((String) user.get("nickName"));
                        userInfo.setMobile((String) user.get("mobile"));
                        userInfo.setEmail((String) user.get("email"));
                        userInfo.setAvatar((String) user.get("avatar"));
                        userInfo.setToken(token);
                        userInfo.setIp(ip);
                        userInfo.setDeviceType(deviceType);
                        listOnlineUser.add(userInfo);
                    }
                } catch (Exception e) {
                    log.warn("解析在线用户会话键失败，已跳过：{}，原因：{}", key, e.getMessage());
                }
            }
        }
        // 会话数据来自 Redis，无数据库分页：按 token 稳定排序后在内存中分页，保证多页数据不重不漏
        listOnlineUser.sort(Comparator.comparing(OnlineUserInfo::getToken));
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
     * 回收失效会话的 Redis 键：令牌、在线元数据、同IP会话索引。
     * 同 IP 索引只在仍指向本令牌、无值或指向的会话本体已不存在（孤儿索引）时删除；
     * 该索引可能已被同 IP 新登录重新指向存活会话，误删会让新会话成为登录踢人扫不到的孤儿。
     */
    private void evictSession(String token, String username) {
        redisUtil.del(AuthConst.PREFIX_USER_TOKEN + token);
        redisUtil.del(AuthConst.PREFIX_ONLINE_META + token);
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
