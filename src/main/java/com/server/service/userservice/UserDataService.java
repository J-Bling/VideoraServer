package com.server.service.userservice;

import com.server.dto.request.UserRequestBase;
import com.server.dto.response.user.UserResponse;
import com.server.entity.user.User;
import java.util.List;

public interface UserDataService {
    UserResponse tryGetUserResponse(Integer targetId);
    UserResponse getUserResponseData(Integer targetId) throws InterruptedException;
    UserResponse getUserDataWithStats(Integer targetId) throws InterruptedException;
    UserResponse getUserWithStatsAndRelation(Integer targetId,Integer userId) throws InterruptedException;
    User getUserData(Integer targetId);
    User getUserData(UserRequestBase requestBase);
    void setUserDataOnCache(User user);
    void setUserResponseOnCache(List<UserResponse> responses);
    void setUserResponseOnCache(UserResponse responses);
}
