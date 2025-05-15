package com.server.push.dto.request;

import com.server.comment.entity.Comment;
import com.server.dto.response.comment.CommentResponse;
import com.server.dto.response.video.VideoDataResponse;

import java.util.List;

public class NotificationForComment extends NotificationForVideoResponse{
    private Comment commentResponse;

    public Comment getCommentResponse() {
        return commentResponse;
    }
    public void setCommentResponse(Comment commentResponse) {
        this.commentResponse = commentResponse;
    }
    public NotificationForComment(){}
    public NotificationForComment(Comment commentResponse, VideoDataResponse videoDataResponse){
        this.commentResponse=commentResponse;
        this.videoDataResponse=videoDataResponse;
    }
}
