package com.server.enums;

import com.server.exception.ApiException;

public enum NotificationType {
    SYSTEM("systme"),
    SERVER("server"),
    USER("user");

    private final String value;

    NotificationType(String value){
        this.value=value;
    }

    public static String formValue(String value){
        for(NotificationType notificationType:values()){
            if(notificationType.getValue().equals(value)){
                return value;
            }
        }

        throw new ApiException(ErrorCode.BAD_REQUEST);
    }

    public String getValue() {
        return value;
    }

}
