package com.server.message.entity;

public class MessageLife {
    private String room_id;
    private Long expire;

    public Long getExpire() {
        return expire;
    }

    public String getRoomId() {
        return room_id;
    }

    public void setExpire(Long expire) {
        this.expire = expire;
    }

    public void setRoomId(String roomId) {
        this.room_id = roomId;
    }
    public MessageLife(){}
    public MessageLife(String roomId,Long expire){this.room_id=roomId;this.expire=expire;}
}
