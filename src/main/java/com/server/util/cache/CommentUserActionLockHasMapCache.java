package com.server.util.cache;
import com.server.entity.cache.comment.CommentUserActionLockResponse;
import com.server.entity.cache.comment.CommentUserLockDto;
import com.server.dto.response.comment.CommentResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CommentUserActionLockHasMapCache{
    private final ConcurrentHashMap<Integer, CommentUserLockDto> Cache=new ConcurrentHashMap<>();
    private final long RETAIN_TIME=10*60;

    public void putCache(CommentUserActionLockResponse response, Integer commentId){
        Cache.computeIfAbsent(commentId,k->new CommentUserLockDto(response)).add(response);
    }

    public void putAll(List<CommentResponse> responses,Integer userId){
        for(CommentResponse response : responses){

            CommentUserActionLockResponse lockResponse=
                    new CommentUserActionLockResponse(userId,response.getAction());

            Cache.computeIfAbsent(response.getId(),k->new CommentUserLockDto(
                    lockResponse
                    )).add(lockResponse);
        }
    }

    public CommentUserLockDto getCommentUserLock(Integer commentId){
        return Cache.computeIfAbsent(commentId,k->new CommentUserLockDto());
    }


    public boolean contains(Integer commentId,int userId){
        CommentUserLockDto comment =Cache.get(commentId);
        if(comment!=null) return comment.contains(userId);
        return false;
    }

    public void remove(Integer commentId,int userId){
        CommentUserLockDto comment =Cache.get(commentId);
        if(comment!=null) comment.remove(userId);
    }

    public void add(Integer commentId,CommentUserActionLockResponse response){
        CommentUserLockDto comment=Cache.get(commentId);
        if(comment!=null){
            comment.add(response);
        }
    }

    public void setActionType(Integer commentId,int userId,boolean type){
        CommentUserLockDto lockDto=Cache.get(commentId);
        if(lockDto!=null) lockDto.setActionType(userId,type);
    }

    public void cleanCache(){
        if(!Cache.isEmpty()) {
            long time = System.currentTimeMillis() / 1000;
            Cache.forEach(1, (key, value) -> {
                if (value != null) {
                    if (time - value.getCacheTime() >= RETAIN_TIME) {
                        Cache.remove(key);
                    }
                } else {
                    Cache.remove(key);
                }
            });
        }
    }
}
