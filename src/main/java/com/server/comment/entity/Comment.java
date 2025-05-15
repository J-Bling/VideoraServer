package com.server.comment.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class Comment {
    private Integer user_id;
    private String comment_id;
    private String content;
    private Integer video_id;
    private String root_id;
    private String parent_id;
    private Integer like_count;
    private Integer reply_count;
    private Long created;

    public Comment(){}
    public Comment(Integer user_id,String content,Integer videoId,String rootId,String parentId){
        this.user_id=user_id;
        this.comment_id=UUID.randomUUID().toString().replace("-","");
        this.content = content;
        this.created=System.currentTimeMillis();
        this.video_id=videoId;
        this.root_id=rootId;
        this.parent_id=parentId;
        this.like_count=0;
        this.reply_count=0;
    }
}
