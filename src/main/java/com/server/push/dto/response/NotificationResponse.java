package com.server.push.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NotificationResponse {
    private String[] notifications;
    private Integer type;

    public NotificationResponse(){}
    public NotificationResponse(String[] notifications){
        this.notifications=notifications;
    }


    public String[] getNotifications() {
        return notifications;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setNotifications(String[] notifications) {
        this.notifications = notifications;
    }
}
