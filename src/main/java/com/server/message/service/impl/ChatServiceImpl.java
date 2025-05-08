package com.server.message.service.impl;

import com.server.dao.user.UserDao;
import com.server.dto.response.user.UserResponse;
import com.server.message.entity.Message;
import com.server.message.service.ChatService;
import com.server.message.service.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired private UserDao userDao;
    @Autowired private ChatWebSocketHandler handler;

    @Override
    public List<Message> findHistoryMessage(Integer userId, Integer targetId, Long lastCreated, int offset) {
        return handler.findHistoryMessage(userId,targetId,lastCreated,offset);
    }

    @Override
    public List<UserResponse> findFriends(Integer userId) {
        return userDao.findFriends(userId);
    }
}
