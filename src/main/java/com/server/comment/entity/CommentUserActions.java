package com.server.comment.entity;
//comment_user_actions
public class CommentUserActions {
    private String comment_id;
    private Integer user_id;

    public CommentUserActions(){}
    public CommentUserActions(String comment_id,Integer user_id){
        this.comment_id=comment_id;
        this.user_id=user_id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public String getComment_id() {
        return comment_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public void setComment_id(String comment_id) {
        this.comment_id = comment_id;
    }
}
