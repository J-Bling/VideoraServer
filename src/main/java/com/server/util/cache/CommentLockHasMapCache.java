package com.server.util.cache;

import com.server.entity.cache.comment.CommentLockResponseDto;
import com.server.dto.response.comment.CommentResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CommentLockHasMapCache {
    private final ConcurrentHashMap<Integer, CommentLockResponseDto> Cache=new ConcurrentHashMap<>();
    protected long RETAIN_TIME=1800;//保留半小时

    public CommentLockResponseDto getLockComment(Integer targetId){
        return Cache.computeIfAbsent(targetId,k->new CommentLockResponseDto());
    }

    public List<CommentResponse> getCommentResponse(Integer targetId){
        CommentLockResponseDto commentLockResponseDto =this.getLockComment(targetId);
        return commentLockResponseDto.getCache();
    }

    public void remove(Integer targetId,Integer commentId){
        CommentLockResponseDto commentLockResponseDto= this.getLockComment(targetId);

        synchronized (commentLockResponseDto.getLOCK()){
            commentLockResponseDto.remove(commentId);
        }
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
