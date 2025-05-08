package com.server.entity.user;

import lombok.Data;

@Data
public class UserRelation {
    private Integer id;
    private Integer  user_id;
    private Integer target_id;//up id
    private Boolean relation_type;//0拉黑 1关注

    public UserRelation(){}
    public UserRelation(int user_id,int target_id,Boolean relation_type){
        this.user_id=user_id;this.target_id=target_id;this.relation_type=relation_type;
    }

    public boolean isVail(){
        return user_id !=null && target_id != null;
    }
    public UserRelation(Boolean relation_type){
        this.relation_type=relation_type;
    }
}
