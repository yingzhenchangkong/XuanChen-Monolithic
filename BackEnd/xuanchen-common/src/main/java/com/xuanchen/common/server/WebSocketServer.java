package com.xuanchen.common.server;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 服务器端
 *
 * @author XuanChen
 * @date 2025-10-21
 */
@Component
//定义websocket服务器端，它的功能主要是将目前的类定义成一个websocket服务器端。注解的值将被用于监听用户连接的终端访问URL地址
@ServerEndpoint("/ws")
public class WebSocketServer {

    private Session session;
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();

    /**
     * 连接成功调用的方法
     *
     * @param session
     */
    @OnOpen
    public void onOpen(Session session) {
        // TODO: 检查权限
        this.session = session;
        webSocketSet.add(this);
        System.out.println("【websocket消息】有新的连接, 当前连接总数:" + webSocketSet.size());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
        System.out.println("【websocket消息】连接断开, 当前连接总数:" + webSocketSet.size());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message) {
        System.out.println("【websocket消息】收到客户端发来的消息:" + message);
    }

    /**
     * 出现错误
     *
     * @param error
     */
    @OnError
    public void onError(Throwable error) {
        error.printStackTrace();
    }

    /**
     * 主动向客户端发送消息
     *
     * @param message
     */
    public static void sendMessage(String message) {
        for (WebSocketServer webSocketServer : webSocketSet) {
            System.out.println("【websocket消息】广播消息:" + message);
            try {
                webSocketServer.session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
