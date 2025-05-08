package com.server.entity.comment;

import com.server.dto.request.comment.CommentRequest;
import lombok.Data;

@Data
public class Comment {
    private Integer id;
    private Integer user_id;
    private Integer video_id;
    private Integer root_id;
    private Integer parent_id;
    private Integer reply_count;
    private Integer like_count;
    private Integer dislike_count;
    private String content;
    private Boolean state;
    private Long created;

    public Comment(){}
    public Comment(CommentRequest commentRequest){
        this.video_id=commentRequest.getVideo_id();
        this.root_id=commentRequest.getRoot_id();
        this.parent_id=commentRequest.getParent_id();
        this.content=commentRequest.getContent();
    }
}
