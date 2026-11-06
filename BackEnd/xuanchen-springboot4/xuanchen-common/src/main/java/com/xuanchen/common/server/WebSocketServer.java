package com.xuanchen.common.server;

import com.xuanchen.common.config.WebSocketAuthConfigurator;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket 服务器端
 * <p>
 * 安全约束：
 * 1、握手阶段由 {@link WebSocketAuthConfigurator} 完成 Origin 白名单与 token 会话校验；
 * 2、{@link #onOpen} 中无用户名（未认证）立即关闭连接，且不加入连接池；
 * 3、服务端消息只按用户名定向推送，不再向所有连接广播，避免跨用户数据泄露；
 * 4、会话在服务端失效（登出/被强制退出/密码重置）时，由业务侧调用
 *    {@link #closeSessionOnLogout} / {@link #closeSessionOnKick} / {@link #closeUserOnKick}
 *    立即断开对应长连接——否则该连接依旧存活、继续接收推送，且"踢出"要等到下一次
 *    HTTP 请求 401 才被客户端感知。
 * <p>
 * 推送模型：业务线程只向有界线程池投递发送任务，{@code AsyncRemote} 异步写出 +
 * 按连接串行 + 单次发送有界超时。任一连接 TCP 写阻塞（慢客户端/半开连接）
 * 只会占住一个推送线程直到超时并被主动断开，不阻塞业务线程与其他连接。
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws", configurator = WebSocketAuthConfigurator.class)
public class WebSocketServer {

    private Session session;
    private String userName;
    /**
     * 握手时携带的访问令牌与会话ID。
     * <p>
     * 用于在"登出/被踢"时精确断开对应的长连接：会话ID在同一会话多次续期
     * （访问令牌轮换）期间保持不变，故优先按会话ID匹配，令牌仅作兜底。
     */
    private String token;
    private String sessionId;
    /**
     * 该连接是否通过认证并被接入连接池。
     * 未认证握手在 {@link #onOpen} 即被关闭、从不入池，其后续 onClose/onError 不应再以
     * “用户[null]连接断开”污染业务日志，也不参与连接总数统计。
     */
    private volatile boolean accepted = false;
    /**
     * 单连接写锁：JSR-356 禁止对同一 Session 并发发送（AsyncRemote 也不例外），按连接串行化
     */
    private final Object sendLock = new Object();

    private static final Set<WebSocketServer> WEB_SOCKET_SET = new CopyOnWriteArraySet<>();

    /**
     * 被拒绝的未认证握手累计数。
     * 历史上出现过旧版客户端（onerror/onclose 双定时器叠加）在服务停机期间堆积出上万定时器，
     * 恢复后形成重连风暴（实测每秒约 60 次），逐条告警会把磁盘与控制台刷爆，故前 3 条逐次告警、
     * 之后每 100 次采样输出一条，既保留现场（是否携带 token）又不让日志爆炸。
     */
    private static final AtomicLong REJECTED_HANDSHAKES = new AtomicLong();

    /**
     * 单次推送最长等待时间；超过即判定为慢客户端，主动断开该连接
     */
    private static final long SEND_TIMEOUT_SECONDS = 5L;

    /**
     * 推送线程池：
     * 有界队列 + CallerRunsPolicy——推送风暴时由调用线程背压执行，绝不无界堆积消息撑爆内存；
     * 守护线程，不阻止 JVM 退出。
     */
    private static final ThreadPoolExecutor ASYNC_SEND_POOL = createSendPool();

    private static ThreadPoolExecutor createSendPool() {
        int cpu = Runtime.getRuntime().availableProcessors();
        AtomicInteger threadIndex = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "ws-async-send-" + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                Math.min(4, Math.max(2, cpu)),
                Math.max(8, cpu * 2),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
        Runtime.getRuntime().addShutdownHook(new Thread(pool::shutdownNow, "ws-async-send-shutdown"));
        return pool;
    }

    /**
     * 连接成功调用的方法：握手用户属性中必须有用户名（来自 token 会话校验）
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws IOException {
        Object userNameObj = config.getUserProperties().get("userName");
        if (!(userNameObj instanceof String loginUser) || loginUser.isBlank()) {
            long rejected = REJECTED_HANDSHAKES.incrementAndGet();
            String query = session.getQueryString();
            boolean hasToken = query != null && query.contains("token=");
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "未认证或登录已过期"));
            if (rejected <= 3 || rejected % 100 == 0) {
                log.warn("【websocket】拒绝未认证握手（累计第{}次{}），是否携带token:{}",
                        rejected, rejected <= 3 ? "" : "，采样日志", hasToken);
            }
            return;
        }
        this.session = session;
        this.userName = loginUser;
        this.token = asText(config.getUserProperties().get("token"));
        this.sessionId = asText(config.getUserProperties().get("sessionId"));
        this.accepted = true;
        WEB_SOCKET_SET.add(this);
        log.info("【websocket消息】用户[{}]建立连接, 当前连接总数:{}", loginUser, WEB_SOCKET_SET.size());
    }

    private static String asText(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        // 未认证握手从未入池：静默丢弃，避免以“用户[null]连接断开”刷屏并干扰连接总数
        if (!accepted) {
            return;
        }
        WEB_SOCKET_SET.remove(this);
        log.info("【websocket消息】用户[{}]连接断开, 当前连接总数:{}", userName, WEB_SOCKET_SET.size());
    }

    /**
     * 应用层心跳报文：浏览器 WebSocket API 无法发送协议级 Ping 帧，客户端定时发文本 "ping"，
     * 服务端原样应答 "pong"，用于：
     * 1）维持 Nginx/网关的空闲读超时（默认 60s 无数据即断）；
     * 2）客户端探测半开/TCP 黑洞连接（限期收不到 pong 即主动断开重连）。
     */
    private static final String HEARTBEAT_PING = "ping";
    private static final String HEARTBEAT_PONG = "pong";

    /**
     * 收到客户端消息：心跳报文应答 pong；其余消息服务端不做任何广播，仅记录
     */
    @OnMessage
    public void onMessage(String message) {
        if (HEARTBEAT_PING.equals(message)) {
            sendAsync(session, HEARTBEAT_PONG);
            return;
        }
        log.debug("【websocket消息】收到用户[{}]的消息:{}", userName, message);
    }

    @OnError
    public void onError(Throwable error) {
        // 未认证连接的异常（被策略关闭时容器可能补一个 error 事件）只留 debug，不刷错误日志
        if (!accepted) {
            log.debug("【websocket】未认证连接异常: {}", error.getMessage());
            return;
        }
        // 客户端单方面断开（关标签页、刷新、进程被杀、切网络、网关空闲超时）在传输层表现为
        // Connection reset / Broken pipe / EOF / ClosedChannel。这属于预期事件：连接已不可用，
        // 后续 @OnClose 会完成出池清理，无需人工介入。此处降级为 debug，避免 ERROR + 完整堆栈
        // 淹没真正的服务端故障；只有其它异常（编码失败、容器内部错误等）才按 error 记录。
        if (isBenignDisconnect(error)) {
            log.debug("【websocket消息】用户[{}]连接被动断开: {}", userName, error.getMessage());
            return;
        }
        log.error("【websocket消息】用户[" + userName + "]连接异常", error);
    }

    /**
     * 判断异常链是否只是"对端异常断开"，用于把噪音日志降级到 debug。
     * <p>
     * Tomcat 常把底层网络异常包一层 IOException 抛出，故必须沿 cause 链判断。
     */
    private static boolean isBenignDisconnect(Throwable error) {
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < 10; depth++) {
            if (cause instanceof EOFException || cause instanceof ClosedChannelException) {
                return true;
            }
            if (cause instanceof SocketException) {
                String message = cause.getMessage();
                if (message == null) {
                    return true;
                }
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("connection reset") || lower.contains("broken pipe")
                        || lower.contains("connection abort") || lower.contains("socket closed")) {
                    return true;
                }
            }
            if (cause == cause.getCause()) {
                break;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 向指定用户的全部在线连接异步推送消息（多终端登录场景）。
     * <p>
     * 该方法立即返回，不等待任何客户端 ACK；池满（大面积慢客户端）时退化为调用线程同步背压，
     * 以牺牲单次调用为代价避免消息无界堆积。
     *
     * @param userName 目标用户名
     * @param message  消息内容
     */
    public static void sendMessage(String userName, String message) {
        if (userName == null || userName.isBlank() || message == null) {
            return;
        }
        for (WebSocketServer webSocket : WEB_SOCKET_SET) {
            Session target = webSocket.session;
            if (userName.equals(webSocket.userName) && target != null && target.isOpen()) {
                ASYNC_SEND_POOL.execute(() -> webSocket.sendAsync(target, message));
            }
        }
    }

    /**
     * 用户主动退出登录：以正常关闭码（1000）断开该会话的长连接。
     * <p>
     * 退出后访问令牌与会话均已撤销，长连接不应继续存在——否则它仍能收到服务端推送，
     * 只有等到"下一次重连"才会被握手校验拦下。正常关闭码不会让前端误判为"会话过期"。
     *
     * @param sessionId 会话ID（为空时按 token 兜底匹配）
     * @param token     本次退出请求携带的访问令牌
     */
    public static void closeSessionOnLogout(String sessionId, String token) {
        closeMatching(sessionId, token,
                new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "退出登录"));
    }

    /**
     * 服务端终止会话（管理员强制退出）：以 1008 断开该会话的长连接。
     * <p>
     * 1008 会被前端识别为"凭证失效、重试无法恢复"：停止自动重连并立即走登录过期流程（跳登录页）。
     * 这样"强制退出"就从"等下一次 HTTP 请求才生效的软踢"变为即时、客户端可见的踢出。
     *
     * @param sessionId 会话ID（为空时按 token 兜底匹配）
     * @param token     该会话的访问令牌（管理员从在线列表拿到的令牌）
     * @param reason    关闭原因，随关闭帧下发给前端用于提示
     */
    public static void closeSessionOnKick(String sessionId, String token, String reason) {
        closeMatching(sessionId, token, new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, kickReason(reason)));
    }

    /**
     * 账号级踢出：断开该用户名下全部长连接（重置密码、账号被停用/删除）
     *
     * @param userName 用户名
     * @param reason   关闭原因
     */
    public static void closeUserOnKick(String userName, String reason) {
        if (userName == null || userName.isBlank()) {
            return;
        }
        CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, kickReason(reason));
        for (WebSocketServer webSocket : WEB_SOCKET_SET) {
            if (userName.equals(webSocket.userName)) {
                webSocket.closeAsync(closeReason);
            }
        }
    }

    private static String kickReason(String reason) {
        return (reason == null || reason.isBlank()) ? "会话已被服务端终止" : reason;
    }

    /**
     * 按"会话ID优先、令牌兜底"匹配并异步关闭连接
     */
    private static void closeMatching(String sessionId, String token, CloseReason reason) {
        boolean bySession = sessionId != null && !sessionId.isBlank();
        if (!bySession && (token == null || token.isBlank())) {
            return;
        }
        for (WebSocketServer webSocket : WEB_SOCKET_SET) {
            boolean matched = bySession
                    ? sessionId.equals(webSocket.sessionId)
                    : token.equals(webSocket.token);
            if (matched) {
                webSocket.closeAsync(reason);
            }
        }
    }

    /**
     * 异步关闭本连接：投递到推送线程池，避免阻塞调用方（HTTP 请求线程）；
     * 关闭失败只记日志，连接仍会由 {@code @OnClose} 负责移出连接池
     */
    private void closeAsync(CloseReason reason) {
        Session target = this.session;
        if (target == null || !target.isOpen()) {
            return;
        }
        ASYNC_SEND_POOL.execute(() -> {
            synchronized (sendLock) {
                try {
                    target.close(reason);
                } catch (IOException ex) {
                    log.debug("【websocket消息】断开连接异常, 用户[{}]", userName, ex);
                }
            }
        });
    }

    /**
     * 在推送线程中向单个连接写出：按连接串行 + AsyncRemote + 有界超时。
     * 同一连接的消息排队、不同连接互不影响；超时连接被主动剔除，防止慢客户端长期占线。
     */
    private void sendAsync(Session target, String message) {
        synchronized (sendLock) {
            if (!target.isOpen()) {
                return;
            }
            Future<Void> future = target.getAsyncRemote().sendText(message);
            try {
                future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                future.cancel(true);
                log.warn("【websocket消息】向用户[{}]推送超过{}秒未完成，判定为慢客户端并关闭连接",
                        userName, SEND_TIMEOUT_SECONDS);
                closeQuietly(target, new CloseReason(
                        CloseReason.CloseCodes.TRY_AGAIN_LATER, "推送超时，连接已被服务端关闭"));
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                log.warn("【websocket消息】向用户[{}]推送消息失败: {}", userName, cause.getMessage());
            } catch (InterruptedException ex) {
                future.cancel(true);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void closeQuietly(Session target, CloseReason reason) {
        try {
            target.close(reason);
        } catch (IOException ex) {
            log.debug("【websocket消息】关闭超时连接异常, 用户[{}]", userName, ex);
        }
    }
}
