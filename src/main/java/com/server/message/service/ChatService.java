package com.server.message.service;

import com.server.dto.response.user.UserResponse;
import com.server.message.entity.Message;
import org.springframework.lang.Nullable;

import java.util.List;

public interface ChatService{
    List<Message> findHistoryMessage(Integer userId, Integer targetId, @Nullable Long lastCreated, int offset);
    List<UserResponse> findFriends(Integer userId);
}
