package com.server.message.entity;
import lombok.Data;

@Data
public class Message {
    private Integer id;
    private String room;
    private Integer sender_id;
    private Integer target_id;
    private String message;
    private Long created; //毫秒

    public Message(){}
    public Message(Integer sender_id,Integer target_id,String message){
        if(sender_id.equals(target_id)) throw new RuntimeException("send_id==target_id");
        this.sender_id=sender_id;
        this.target_id=target_id;
        this.message=message;
        this.room=sender_id<target_id ?sender_id+":"+target_id : target_id+":"+sender_id;
        this.created=System.currentTimeMillis();
    }
}
