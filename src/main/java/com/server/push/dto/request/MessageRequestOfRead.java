package com.server.push.dto.request;

import java.util.ArrayList;
import java.util.List;

public class MessageRequestOfRead {
    private String userId;
    private List<String> messageIds;

    public void collection(){
        List<String> ids=new ArrayList<>();
        for(String id : messageIds){
            if(id!=null) ids.add(id);
        }
        messageIds=ids;
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getMessageIds() {
        return messageIds;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }
}
