package com.server.message.service.impl;

import com.server.dao.message.MessageDao;
import com.server.dao.user.UserDao;
import com.server.dto.response.user.UserResponse;
import com.server.message.entity.Message;
import com.server.message.service.ChatService;
import com.server.message.service.ChatWebSocketHandler;
import com.server.push.enums.NotificationCode;
import com.server.push.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired private UserDao userDao;
    @Autowired private ChatWebSocketHandler handler;
    @Autowired private MessageDao messageDao;
    @Autowired private NotificationService notificationService;

    @Override
    public List<Message> findHistoryMessage(Integer userId, Integer targetId, Long lastCreated, int offset) {
        return handler.findHistoryMessage(userId,targetId,lastCreated,offset);
    }

    /**
     * 搜索好友列表 1先从我关注列表里里面查找
     * 2从通过notification type=4来通知关注我的人中查找 3从通知notification type=5私信过我的人中查找 在客户端先查关注列表的数据 后补充
     *
     */

    @Override
    public List<UserResponse> findFriends(Integer userId) {
        return userDao.findFriends(userId);
    }

    /**
     * 定期清理 notification created 超过3个月已读消息 超过1年未读消息
     * 定期清理 message 把过去3个月未读消息进行清理 对一年内未读消息进行清理
     * 因为是两个人共同拥有的数据 不能单方面删除消息 但可以清空缓存和通知
     */
    @Override
    public void deleteMessage(int userId,int targetId) {
        notificationService.deleteNotifications(userId,targetId);
    }
}
