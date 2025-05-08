package com.server.service.userservice;

import com.server.dto.request.UserRequestBase;
import com.server.entity.user.VerificationCode;


public interface VerificationCodeService {
    void sendCode(UserRequestBase userRequestBase);
}
