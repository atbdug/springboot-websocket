package com.gj.sendone;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gjing
 **/
@ServerEndpoint("/test-one/{chatId}")
@Component
@Slf4j
public class MyOneToOneServer {
    /**
     * 用于存放所有在线客户端
     */
    private final static  Map<String, Session> clients = new ConcurrentHashMap<>();
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static AtomicInteger onlineNum = new AtomicInteger();
    private Session session;
    private final Gson gson = new Gson();

    @OnOpen
    public void onOpen(Session session) {
        log.info("有新的客户端上线: {}", session.getId());
        this.session = session;
        clients.put(session.getId(), session);
        int num = onlineNum.incrementAndGet();
        System.out.println("当前在线人数："+num+"人");
    }

    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        log.info("有客户端离线: {}", sessionId);
        clients.remove(sessionId);
        int num = onlineNum.decrementAndGet();
        System.out.println("当前在线人数："+num+"人");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        String sessionId = session.getId();
        if (clients.get(sessionId) != null) {
            log.info("发生了错误,移除客户端: {}", sessionId);
            clients.remove(sessionId);
        }
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message,Session session) {
        log.info("收到客户端发来的消息: {}", message);
        String userId = session.getId();
        Message messageBody = new Message();
        JSONObject jsonObject = JSONObject.parseObject(message);
        messageBody.setMessage(jsonObject.getString("msg"));
        messageBody.setToUserId(jsonObject.getString("toUserId"));
        messageBody.setUserId(session.getId());
        for (Map.Entry<String, Session> entry: this.clients.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            if (entry.getKey().equals(messageBody.getToUserId())){
                messageBody.setUserId(entry.getKey());
                this.sendTo(entry.getValue(),messageBody);
            }
        }
    }

    /**
     * 发送消息
     *
     * @param message 消息对象
     */
    private void sendTo(Session session,Message message) {
        Session s = clients.get(message.getUserId());
        if (s != null) {
            try {
                if (s.getId().equals(message.getUserId())){
                    s.getBasicRemote().sendText(message.getMessage());
                }else {
                    s.getAsyncRemote().sendText(message.getMessage());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
