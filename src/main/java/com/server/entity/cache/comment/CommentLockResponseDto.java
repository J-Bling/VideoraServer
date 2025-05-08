package com.server.entity.cache.comment;

import com.server.dto.response.comment.CommentResponse;

import java.util.ArrayList;
import java.util.List;

public class CommentLockResponseDto {
    private Long cacheTime;
    private List<CommentResponse> cache;
    private Object LOCK=new Object();

    public CommentLockResponseDto(List<CommentResponse> cache) {
        this.cache=cache;
        this.cacheTime=System.currentTimeMillis()/1000;
    }

    public CommentLockResponseDto(){
        this.cache=new ArrayList<>();
        this.cacheTime=System.currentTimeMillis()/1000;
    }

    public Long getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(Long cacheTime) {
        this.cacheTime = cacheTime;
    }

    public List<CommentResponse> getCache() {
        return cache;
    }

    public void setCache(List<CommentResponse> cache) {
        this.cache=cache;
    }

    public void addCache(List<CommentResponse> cache){
        if(cache!=null && !cache.isEmpty()){
            this.cache.addAll(cache);
        }
    }

    public Object getLOCK() {
        return LOCK;
    }

    public void remove(Integer commentId) {
        try {
            if(cache.isEmpty()){
                return;
            }

            for (CommentResponse commentResponse : cache) {
                if (commentResponse.getId().equals(commentId)) {
                    cache.remove(commentResponse);
                    return;
                }
            }
        }catch (Exception e){
            return;
        }
    }

}
