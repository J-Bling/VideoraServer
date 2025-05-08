package com.server.service.userservice;

import com.server.controller.api.user.UserDataController;
import com.server.dto.request.auth.AuthRequest;
import com.server.entity.user.User;

public interface UserService {
    boolean deleteUser(int id);
    boolean isAdmin(String sub);
    boolean isLock(int sub);


    Integer createUser(AuthRequest request);
    String loginByPass(AuthRequest request);
    String loginByCode(AuthRequest request);

    User loginByPassGetUser(AuthRequest request);
    User loginByCodeGetUser(AuthRequest request);

    void resetNickname(int userId,String nickname);
    void resetPasswordByPass(int userId, UserDataController.ResetPasswordRequest passwordRequest);
    void resetPasswordByCode(int userId,UserDataController.ResetPasswordRequest passwordRequest);
    void resetUserAvatar(int userId,String avatar_url);
}
