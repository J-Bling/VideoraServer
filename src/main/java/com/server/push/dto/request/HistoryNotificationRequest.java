package com.server.push.dto.request;

import com.server.push.enums.NotificationCode;
import lombok.Data;

@Data
public class HistoryNotificationRequest {
    private Integer userId;
    private Integer type;

    private boolean isVail(){
        return userId!=null && NotificationCode.isVailCode(type);
    }

    public HistoryNotificationRequest(){}
    public HistoryNotificationRequest(Integer userId,Integer type){
        this.userId=userId;this.type=type;
    }
}
