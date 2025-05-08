package com.server.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.server.dto.response.user.UserResponse;
import lombok.Data;
import lombok.ToString;

import java.sql.Timestamp;

@Data
public class CommentResponse {
    private Integer id;
    private Integer video_id;
    private Integer root_id;
    private Integer parent_id;
    private int reply_count;
    private int like_count;
    private int dislike_count;
    private String content;
    private Timestamp created;
    private UserResponse user;
    private Boolean action;

    @Data
    public static class CommentCacheResponse{
        private Integer id;
        private Integer video_id;
        private Integer root_id;
        private Integer parent_id;
        private int reply_count;
        private int like_count;
        private int dislike_count;
        private String content;
        private Timestamp created;
        private UserResponse user;
        private Boolean action;

        public CommentCacheResponse(){}
        public CommentCacheResponse(CommentResponse commentResponse){
            this.id = commentResponse.getId();
            this.video_id = commentResponse.getVideo_id();
            this.root_id = commentResponse.getRoot_id();
            this.parent_id = commentResponse.getParent_id();
            this.reply_count = commentResponse.getReply_count();
            this.like_count = commentResponse.getLike_count();
            this.dislike_count = commentResponse.getDislike_count();
            this.content = commentResponse.getContent();
            this.created = commentResponse.getCreated();
            this.user = commentResponse.getUser();
            this.action = commentResponse.getAction();
        }
    }

    public static CommentCacheResponse toCommentCacheResponse(CommentResponse commentResponse){
        return new CommentCacheResponse(commentResponse);
    }

    @JsonIgnore
    @ToString.Exclude
    private final Object LOCK=new Object();


    public void updateReplyCount(int count){
        synchronized (LOCK){
            this.reply_count+=count;
        }
    }

    public void updateLikeCount(int count){
        synchronized (LOCK){
            this.like_count+=count;
        }
    }

    public void updateDisLikeCount(int count){
        synchronized (LOCK){
            this.dislike_count+=count;
        }
    }

    public void updateAction(Boolean type){
        synchronized (LOCK){
            this.action=type;
        }
    }

    public static CommentResponse copyCommentResponse(CommentResponse commentResponse){
        if(commentResponse==null) return null;

        CommentResponse commentResponse1=new CommentResponse();
        commentResponse1.setId(commentResponse.getId());
        commentResponse1.setRoot_id(commentResponse.getRoot_id());
        commentResponse1.setParent_id(commentResponse1.getParent_id());
        commentResponse1.setReply_count(commentResponse.getReply_count());
        commentResponse1.setLike_count(commentResponse.getLike_count());
        commentResponse1.setDislike_count(commentResponse.getDislike_count());
        commentResponse1.setContent(commentResponse.getContent());
        commentResponse1.setCreated(commentResponse.getCreated());
        commentResponse1.setUser(commentResponse.getUser());
        return commentResponse1;
    }

}
