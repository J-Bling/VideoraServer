package com.server.message.service;

import com.server.message.entity.Message;
import org.springframework.web.socket.WebSocketHandler;

import java.util.List;

public interface ChatWebSocketHandler extends WebSocketHandler {
    List<Message> findHistoryMessage(Integer userId, Integer targetId, Long lastCreated, int offset);
    void produceMessage(Message message);
    void produceMessage(List<Message> messages);
}
