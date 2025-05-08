package com.server.push.dto.request;

import com.server.dto.response.comment.CommentResponse;
import com.server.dto.response.video.VideoDataResponse;

import java.util.List;

public class NotificationForComment extends NotificationForVideoResponse{
    private CommentResponse commentResponse;

    public CommentResponse getCommentResponse() {
        return commentResponse;
    }

    public void setCommentResponse(CommentResponse commentResponse) {
        this.commentResponse = commentResponse;
    }

    public NotificationForComment(){}

    public NotificationForComment(CommentResponse commentResponse, VideoDataResponse videoDataResponse){
        this.commentResponse=commentResponse;
        this.videoDataResponse=videoDataResponse;
    }
}
