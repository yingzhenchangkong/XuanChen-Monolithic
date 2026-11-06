package com.xuanchen.auth.shiro.exception;

import org.apache.shiro.authc.AuthenticationException;

/**
 * JWT 认证失败异常：携带结构化的失败原因，替代"所有异常统一回 Token失效"的做法。
 * <p>
 * 过滤器据此向客户端返回可区分的提示（过期/无效/冻结/账号不存在），
 * 同时服务端按 {@link Reason} 记录日志，便于排障与攻击行为识别。
 *
 * @author XuanChen
 * @date 2026-10-01
 */
public class JwtAuthException extends AuthenticationException {

    /**
     * 认证失败原因
     */
    public enum Reason {
        /**
         * 请求未携带 token
         */
        TOKEN_EMPTY("未检测到登录凭证，请先登录！"),
        /**
         * token 无法解析（格式非法/被篡改）
         */
        TOKEN_MALFORMED("登录凭证无效，请重新登录！"),
        /**
         * JWT 自身已过 exp 有效期
         */
        TOKEN_EXPIRED("登录状态已过期，请重新登录！"),
        /**
         * 签名校验失败（签名错/算法不符/claim 不匹配）
         */
        SIGNATURE_INVALID("登录凭证无效，请重新登录！"),
        /**
         * Redis 中无对应会话（已登出/被顶号/会话超时）
         */
        SESSION_EXPIRED("登录状态已过期，请重新登录！"),
        /**
         * token 对应用户不存在或已被删除
         */
        USER_NOT_FOUND("账号不存在或已被删除！"),
        /**
         * 账号被管理员冻结
         */
        USER_FROZEN("账号已被冻结，请联系管理员！");

        private final String clientMessage;

        Reason(String clientMessage) {
            this.clientMessage = clientMessage;
        }

        public String getClientMessage() {
            return clientMessage;
        }
    }

    private final Reason reason;

    public JwtAuthException(Reason reason) {
        super(reason.getClientMessage());
        this.reason = reason;
    }

    public JwtAuthException(Reason reason, Throwable cause) {
        super(reason.getClientMessage(), cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
