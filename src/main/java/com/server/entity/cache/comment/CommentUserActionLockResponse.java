package com.server.entity.cache.comment;

import lombok.Data;

@Data
public class CommentUserActionLockResponse {
    private int user_id;
    private Boolean action_type;
    private final Object LOCK=new Object();


    public CommentUserActionLockResponse(int user_id,Boolean action_type){
        this.user_id=user_id;
        this.action_type=action_type;
    }

    public void updateActionType(Boolean action_type){
        synchronized (LOCK){
            this.action_type=action_type;
        }
    }//带锁

}
