package com.server.comment.dto.response;


import com.server.comment.entity.Comment;
import com.server.comment.entity.CommentUserActions;
import com.server.dto.response.user.UserResponse;

public class CommentResponse {
    private Comment comment;
    private UserResponse author;
    private CommentUserActions action;

    public CommentResponse(){}
    public CommentResponse(Comment comment,UserResponse userResponse,CommentUserActions action){
        this.comment=comment;
        this.author=userResponse;
        this.action=action;
    }

    public CommentUserActions getAction() {
        return action;
    }

    public Comment getComment() {
        return comment;
    }

    public UserResponse getAuthor() {
        return author;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public void setAction(CommentUserActions action) {
        this.action = action;
    }

    public void setAuthor(UserResponse author) {
        this.author = author;
    }
}
