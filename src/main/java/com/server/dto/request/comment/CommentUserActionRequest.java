package com.server.dto.request.comment;


import com.server.enums.ErrorCode;
import com.server.exception.ApiException;

public class CommentUserActionRequest {
    private int comment_id;
    private Integer user_id;
    private Boolean action_type;
    private Integer videoId;
    private Integer rootId;
    private Integer authorId;

    public void vailColumn(){
        if(!(user_id!=null && videoId !=null)){
            rootId=rootId!=null ? rootId : 0;
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
    }

    public CommentUserActionRequest(){}
    public CommentUserActionRequest(Integer comment_id,Integer user_id,Boolean action_type){
        this.comment_id=comment_id;this.user_id=user_id;this.action_type=action_type;
    }
    public CommentUserActionRequest(Integer video,Integer rootId,int comment_id,Integer user_id,Boolean action_type){
        this.videoId=video;this.rootId=rootId;
        this.action_type=action_type;this.comment_id=comment_id;this.user_id=user_id;
    }

    public int getComment_id() {
        return comment_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setComment_id(int comment_id) {
        this.comment_id = comment_id;
    }


    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public Boolean isAction_type() {
        return action_type;
    }

    public void setAction_type(Boolean action_type) {
        this.action_type = action_type;
    }
    public void setAction_type(boolean action_type){
        this.action_type=action_type;
    }

    public Boolean getAction_type() {
        return action_type;
    }

    public Integer getVideoId() {
        return videoId;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public Integer getRootId() {
        return rootId;
    }

    public void setRootId(Integer rootId) {
        this.rootId = rootId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

}
