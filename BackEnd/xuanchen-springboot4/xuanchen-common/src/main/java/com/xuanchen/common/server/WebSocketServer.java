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

import java.io.IOException;
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
 * 3、服务端消息只按用户名定向推送，不再向所有连接广播，避免跨用户数据泄露。
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
        this.accepted = true;
        WEB_SOCKET_SET.add(this);
        log.info("【websocket消息】用户[{}]建立连接, 当前连接总数:{}", loginUser, WEB_SOCKET_SET.size());
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
        log.error("【websocket消息】用户[" + userName + "]连接异常", error);
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
