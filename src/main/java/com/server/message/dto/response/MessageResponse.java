package com.server.message.dto.response;

public class MessageResponse {
    private Integer send_id;
    private String message;
    private Long created;

    public MessageResponse (){}

    public String getMessage() {
        return message;
    }

    public Integer getSend_id() {
        return send_id;
    }

    public Long getCreated() {
        return created;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public void setSend_id(Integer send_id) {
        this.send_id = send_id;
    }
}
