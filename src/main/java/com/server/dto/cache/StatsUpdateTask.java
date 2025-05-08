package com.server.dto.cache;

import com.server.entity.constant.CommentConstant;
import lombok.Data;

@Data
public class StatsUpdateTask {
    private Integer targetId;
    private String column;
    private Integer count=0;
    private Object LOCK=new Object();


    public StatsUpdateTask(){}
    public StatsUpdateTask(Integer count,Integer targetId,String column){
        this.column=column;
        this.targetId=targetId;
        this.count=count;
    }

    public void updateCount(int count){
        synchronized (LOCK){
            this.count+=count;
        }
    }

    public void addCount(int count){
        this.count=this.count+count;
    }

    public static StatsUpdateTask forUser(Integer userId, String column, Integer count) {
        CommentConstant.validateColumn(CommentConstant.USER_STATS_TABLE, column);
        StatsUpdateTask task = new StatsUpdateTask();
        task.setTargetId(userId);
        task.setColumn(column);
        task.setCount(count);
        return task;
    }


    public static StatsUpdateTask forVideo(Integer videoId, String column, Integer count) {
        CommentConstant.validateColumn(CommentConstant.VIDEO_STATS_TABLE, column);
        StatsUpdateTask task = new StatsUpdateTask();
        task.setTargetId(videoId);
        task.setColumn(column);
        task.setCount(count);
        return task;
    }

    public static StatsUpdateTask forComment(Integer commentId,String column,Integer count){
        CommentConstant.validateCommentColumn(CommentConstant.COMMENT_TABLE,column);
        return new StatsUpdateTask(count,commentId,column);
    }

    public static StatsUpdateTask forCommentUSerAction(Integer commentId,String column,Integer count){
        CommentConstant.validateCommentUserActionColumn(CommentConstant.COMMENT_USER_ACTION_TABLE,column);
        return new StatsUpdateTask(count,commentId,column);
    }
}