package com.server.entity.cache.comment;

import java.util.ArrayList;
import java.util.List;

public class CommentUserLockDto {
    private final List<CommentUserActionLockResponse> Actions;
    private final long cacheTime;
    private final Object LOCK;

    public CommentUserLockDto(){
        this.Actions=new ArrayList<>();
        this.LOCK=new Object();
        this.cacheTime=System.currentTimeMillis()/1000;
    }
    public CommentUserLockDto(CommentUserActionLockResponse commentUserActionLockResponse){
        this();
        this.add(commentUserActionLockResponse);
    }
    public long getCacheTime() {
        return cacheTime;
    }

    public boolean contains(CommentUserActionLockResponse commentUserActionLockResponse){
        if (commentUserActionLockResponse == null) return false;
        for(CommentUserActionLockResponse userActionLockResponse : Actions){
            if(userActionLockResponse.getUser_id()==commentUserActionLockResponse.getUser_id()){
                return true;
            }
        }
        return false;
    }

    public boolean contains(int userId){
        return this.find(userId)!=null;
    }

    public CommentUserActionLockResponse find(int userId){
        for(CommentUserActionLockResponse userActionLockResponse : Actions){
            if(userActionLockResponse.getUser_id()==userId){
                return userActionLockResponse;
            }
        }
        return null;
    }

    public void remove(int userId){
        synchronized (LOCK){
            CommentUserActionLockResponse response =this.find(userId);
            if(response!=null)
                Actions.remove(response);
        }
    }//带锁

    public void add(CommentUserActionLockResponse commentUserActionLockResponse){
        if(commentUserActionLockResponse==null) return ;
        synchronized (LOCK){
            if(!this.contains(commentUserActionLockResponse)){//不重复添加
                Actions.add(commentUserActionLockResponse);
            }
        }
    }//带锁

    public void add(CommentUserActionLockResponse commentUserActionLockResponse,int x){
        if(commentUserActionLockResponse==null) return ;
        Actions.add(commentUserActionLockResponse);
    }

    public Object getLOCK() {
        return LOCK;
    }

    public List<CommentUserActionLockResponse> getActions() {
        return Actions;
    }

    public void setActionType(int userId, Boolean action){
        CommentUserActionLockResponse response = this.find(userId);
        if(response!=null) response.updateActionType(action);
    }
}
