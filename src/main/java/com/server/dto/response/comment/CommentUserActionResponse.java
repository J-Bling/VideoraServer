package com.server.dto.response.comment;

public class CommentUserActionResponse {
    private Boolean action_type;

    public CommentUserActionResponse(Boolean action_type){
        this.action_type=action_type;
    }
    public CommentUserActionResponse(){}
    public Boolean getAction_type() {
        return action_type;
    }
    public void setAction_type(Boolean action_type) {
        this.action_type = action_type;
    }
}
