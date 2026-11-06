package com.xuanchen.common.server;

/**
 * WebSocket 鉴权桥接器
 * <p>
 * common 模块不能依赖 auth 模块，WebSocket 握手配置器位于 common，
 * 而完整的会话校验（Redis 会话存在性、JWT 签名、账号 delFlag/status）位于 auth。
 * 由 auth 侧在启动时把认证实现注册进来，使 WebSocket 握手与 HTTP 过滤器
 * （ShiroRealm）执行同一套校验，避免出现"HTTP 已拒绝但 WS 仍可匿名连接"的两套口径。
 *
 * @author XuanChen
 * @date 2026-09-29
 */
public final class WsAuthBridge {

    private WsAuthBridge() {
    }

    /**
     * 握手校验通过的会话上下文
     *
     * @param userName  用户名
     * @param sessionId 会话ID（与 HTTP 会话/终端同一标识；历史会话可能为空）。
     *                  会话ID在同一会话多次续期（访问令牌轮换）期间保持不变，
     *                  故"登出/强制退出"按它精确断开对应的 WebSocket 长连接。
     */
    public record SessionContext(String userName, String sessionId) {
    }

    public interface TokenVerifier {
        /**
         * 校验握手携带的令牌。
         *
         * @param token 查询参数中的 JWT
         * @return 校验通过返回会话上下文；令牌无效/会话不存在/账号已删除或冻结时返回 null
         */
        SessionContext verify(String token);
    }

    private static volatile TokenVerifier tokenVerifier;

    public static void registerTokenVerifier(TokenVerifier verifier) {
        tokenVerifier = verifier;
    }

    /**
     * 校验令牌；校验器未注册（理论上不会发生）时保守拒绝
     */
    public static SessionContext verify(String token) {
        TokenVerifier verifier = tokenVerifier;
        return verifier == null ? null : verifier.verify(token);
    }
}
