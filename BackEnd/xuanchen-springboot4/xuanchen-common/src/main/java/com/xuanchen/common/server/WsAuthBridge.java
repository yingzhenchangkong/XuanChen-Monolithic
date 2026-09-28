package com.xuanchen.common.server;

/**
 * WebSocket 鉴权桥接器
 * <p>
 * common 模块不能依赖 auth 模块，WebSocket 握手配置器位于 common，
 * 而 JWT 签名校验逻辑位于 auth。由 auth 侧在启动时把校验实现注册进来，
 * 使 WebSocket 握手与 HTTP 过滤器执行同一套"Redis 会话 + JWT 签名"双重校验。
 *
 * @author XuanChen
 * @date 2026-09-18
 */
public final class WsAuthBridge {

    private WsAuthBridge() {
    }

    public interface TokenVerifier {
        /**
         * @param token    握手携带的 JWT
         * @param userName 从 Redis 会话元数据中解析出的用户名
         * @return 签名/过期/用户名是否合法
         */
        boolean verify(String token, String userName);
    }

    private static volatile TokenVerifier tokenVerifier;

    public static void registerTokenVerifier(TokenVerifier verifier) {
        tokenVerifier = verifier;
    }

    /**
     * 校验 token 签名；校验器未注册（理论上不会发生）时保守拒绝
     */
    public static boolean verify(String token, String userName) {
        TokenVerifier verifier = tokenVerifier;
        return verifier != null && verifier.verify(token, userName);
    }
}
