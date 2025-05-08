package com.server.push.dto.response;

import com.server.dto.response.user.UserResponse;
import com.server.push.entity.Notification;
import lombok.Data;

@Data
public class HistoryNotificationResponse<V>{
    private Notification notifications;
    private UserResponse other;
    private V context;

    public HistoryNotificationResponse(){}
    public HistoryNotificationResponse(Notification notifications,UserResponse userResponse){
        this.notifications=notifications;this.other =userResponse;
    }
}
