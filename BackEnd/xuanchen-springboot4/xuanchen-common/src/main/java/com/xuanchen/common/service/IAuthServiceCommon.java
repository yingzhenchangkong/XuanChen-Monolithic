package com.xuanchen.common.service;

import java.util.Map;

/**
 * Service接口-->认证、授权 通用
 *
 * @author XuanChen
 * @date 2026-02-05
 */
public interface IAuthServiceCommon {
    /**
     * 加密密码
     *
     * @param password
     * @return
     */
    String encryptPassword(String password);

    /**
     * 获取用户名
     *
     * @param token
     * @return
     */
    String getUserNameByToken(String token);

    /**
     * 获取用户信息
     *
     * @param userName
     * @return
     */
    Map<String, Object> getUserByUserName(String userName);

    /**
     * 校验用户原密码是否正确
     *
     * @param userName    用户名（仅允许取自服务端认证主体）
     * @param rawPassword 前端传入的明文原密码
     * @return true 正确 false 用户不存在或密码错误
     */
    boolean checkPassword(String userName, String rawPassword);

    /**
     * 踢掉指定用户的全部在线会话
     * 删除 Redis 中该用户所有终端/IP 的 token 及索引，使其必须重新登录
     *
     * @param userName 用户名
     */
    void kickOutUser(String userName);

    /**
     * 为已通过身份校验的用户签发一套全新的登录会话（与 /login 完全相同的 token 生成、
     * 同 IP 踢下线、Redis 元数据写入与失败计数清理逻辑）。
     * 适用场景：本人修改密码成功后需要免输入直接进入系统（旧密码会话已被踢出，
     * 不能再走 /login，否则开启验证码时会被验证码拦住）。
     *
     * @param userName   用户名（仅允许取自服务端认证主体/已通过原密码校验）
     * @param clientIp   客户端 IP
     * @param deviceType 终端类型
     * @return 与登录响应一致的用户信息（含 token、pwdResetRequired）；用户不存在时返回 null
     */
    Map<String, Object> issueLoginSession(String userName, String clientIp, String deviceType);
}
