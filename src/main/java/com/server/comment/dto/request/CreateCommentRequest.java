package com.server.comment.dto.request;

import com.server.comment.entity.Comment;
import lombok.Data;

@Data
public class CreateCommentRequest {
    private String content;
    private Integer video_id;
    private String root_id;
    private String parent_id;
    private Integer targetId;

    public boolean vail(){
        if(content==null || content.isEmpty() || video_id==null){
            return false;
        }
        if(root_id !=null && parent_id!=null){
            return root_id.length()!=32 || parent_id.length()!=32;
        }
        return root_id==null && parent_id==null;
    }

   public Comment toComment(Integer authorId){
        return new Comment(authorId,content,video_id,root_id,parent_id);
    }
}
