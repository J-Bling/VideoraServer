package com.server.enums;

import org.springframework.web.socket.CloseStatus;

public enum WSStatusCode {
    SINGLE_USERID_ALLOWED(
            4000, "Single session allowed per user"
    ),SESSION_ALREADY_IN_USER(
            1008,"Session already in use"
    ),NOT_ACCEPTABLE(1003,"Binary data not supported");


    private final Integer code;
    private final String description;
    WSStatusCode(Integer code,String description){
        this.code=code;this.description=description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
