package com.server.message.dto.request;

public class SendMessageRequest {
    private Integer target_id;
    private String message;

    public String getMessage() {
        return message;
    }

    public Integer getTarget_id() {
        return target_id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTarget_id(Integer target_id) {
        this.target_id = target_id;
    }
}
