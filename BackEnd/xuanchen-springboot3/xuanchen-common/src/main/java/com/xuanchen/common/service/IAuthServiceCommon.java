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
     * 使用 BCrypt 慢哈希加密口令（盐内嵌于哈希串，无需外部盐）
     *
     * @param rawPassword 明文口令
     * @return BCrypt 哈希（$2a$ 前缀）
     */
    String encryptPassword(String rawPassword);

    /**
     * 校验指定用户的明文口令是否正确（双栈：BCrypt 与历史 SHA-512+盐）
     *
     * @param userName    用户名
     * @param rawPassword 明文口令
     * @return true 正确；用户不存在或口令错误返回 false
     */
    boolean checkPassword(String userName, String rawPassword);

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
     * @return 用户信息 Map（含 delFlag/status）；用户不存在时返回 null
     */
    Map<String, Object> getUserByUserName(String userName);

    /**
     * 读取访问令牌所属会话的会话ID（会话元数据 PREFIX_ONLINE_META_ 中记录）
     * <p>
     * 用于登出/强制退出时按会话精确断开对应的 WebSocket 长连接。
     *
     * @param token 访问令牌
     * @return 会话ID；令牌无可读元数据或为历史会话（无 sessionId）时返回 null
     */
    String getSessionIdByToken(String token);

    /**
     * 撤销指定用户全部"记住我"刷新令牌（账号级，所有终端）
     * <p>
     * 刷新令牌是长效凭据（Redis 值与会话/终端绑定），有效期内可凭 httpOnly Cookie 静默换发新访问令牌。
     * 因此任何"踢人"动作（改密、重置密码、停用/删除账号）在删除访问令牌后，都必须同步撤销刷新令牌；
     * 否则被踢客户端下一次请求 401 时会自动调用 /auth/refresh 换回新令牌，
     * 前端表现为"点了强制退出却仍处于登录态"（勾选"记住我"时必现）。
     * <p>
     * 注意：仅"强制退出单条会话"不应调用本方法（会连带撤销该账号其他终端的刷新令牌），
     * 应改用 {@link #revokeRefreshTokensBySession(String, String)}。
     *
     * @param userName 用户名
     */
    void revokeRefreshTokens(String userName);

    /**
     * 撤销"指定用户 + 指定会话"的刷新令牌（会话级，精确到单条会话/终端）
     * <p>
     * 用于在线用户"强制退出"：只让被踢的那一条会话下次刷新失效，同账号在其他终端的
     * "记住我"会话不受影响。
     *
     * @param userName  用户名
     * @param sessionId 会话ID（签发会话时生成，同时写入会话元数据与刷新令牌值）
     */
    void revokeRefreshTokensBySession(String userName, String sessionId);

    /**
     * 踢掉指定用户"除保留会话外"的全部在线会话
     * <p>
     * 对每条被踢会话一并完成：删除访问令牌、在线会话元数据与同 IP 索引，撤销其绑定的"记住我"
     * 刷新令牌，并断开其 WebSocket 长连接（1008）。
     * <p>
     * 用于"本人修改密码"：用户并未退出登录，当前客户端那条会话必须继续可用——改密成功后前端会拿到
     * 新访问令牌，而浏览器上那枚绑定同一会话ID的"记住我"刷新 Cookie 也必须保持有效，
     * 否则用户关掉浏览器再打开就被迫重新登录（"记住我"形同失效）；
     * 同时该账号在其他终端/其他 IP 的旧会话必须立即失效。
     *
     * @param userName      用户名
     * @param keepSessionId 需要保留的会话ID（当前客户端会话）。为空表示无法识别当前会话，
     *                      此时踢掉全部会话并撤销该账号全部刷新令牌（保守兜底，宁可多踢）
     * @param reason        踢出原因，透传给前端用于提示
     */
    void kickOutOtherSessions(String userName, String keepSessionId, String reason);

    /**
     * 强制下线整个账号（账号停用/删除、密码重置等场景）
     * <p>
     * 一次性完成三件事：撤销该账号全部"记住我"刷新令牌 → 断开全部 WebSocket 长连接 →
     * 删除全部在线会话（访问令牌 + 元数据 + 同 IP 索引）。
     * <p>
     * 账号级失效必须走本方法：只删访问令牌时，客户端会凭 httpOnly 刷新 Cookie 静默换发新访问令牌
     * （勾选"记住我"时必现），表现为"停用了却还能继续用"；不断开长连接时，服务端仍会向已失效会话推送消息。
     *
     * @param userName 用户名
     * @param reason   下线原因，透传给前端用于提示
     */
    void kickOutAccount(String userName, String reason);

    /**
     * 获取当前登录线程（Shiro Subject）中的用户名，用于审计字段等无 HTTP 请求上下文的场景
     *
     * @return 用户名；无登录上下文（未登录/异步线程）时返回空串
     */
    String getCurrentUserName();

    /**
     * 改密成功后轮换当前会话：作废旧令牌的全部会话键，并用库内最新口令哈希签发新会话。
     * 旧令牌因 JWT 以口令哈希签名，改密后验签必然失败；本方法同时清理其 Redis 残留并补发新令牌，
     * 使前端改密成功后无需重新登录。
     *
     * @param userName       用户名
     * @param clientIp       客户端 IP（同 IP 会话索引）
     * @param deviceType     终端类型（会话元数据）
     * @param revokeToken    需作废的旧令牌（当前请求携带的令牌），可为空
     * @return 与 /login 一致的用户信息 Map（含新 token、pwdResetRequired）；用户不存在返回 null
     */
    Map<String, Object> rotateSession(String userName, String clientIp, String deviceType, String revokeToken);
}
