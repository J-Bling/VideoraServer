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

    /**
     * 搜索好友列表 1先从我关注列表里里面查找
     * 2从通过notification type=4来通知关注我的人中查找 3从通知notification type=5私信过我的人中查找 在客户端先查关注列表的数据 后补充
     * 定期清理 notification type=4,5 created 超过3个月一读消息
     */

    @Override
    public List<UserResponse> findFriends(Integer userId) {
        return userDao.findFriends(userId);
    }
}
