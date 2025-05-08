package com.server.dto.response.user;
import com.server.entity.user.User;
import com.server.entity.user.UserRelation;
import com.server.entity.user.UserStats;
import lombok.Data;

@Data
public class UserResponse {

    private Integer id;
    private String nickname;
    private Boolean gender;
    private String avatar_url;
    private String description;
    private UserRelation userRelation; //userRelation==null时即时自己
    private UserStats userStats;

    public UserResponse(){}
    public UserResponse(Integer id,String nickname,Boolean gender,String avatar_url,String description){
        this.id=id;this.nickname=nickname;this.gender=gender;this.avatar_url=avatar_url;this.description=description;
    }
    public User toEntity(){
        User user= new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setGender(gender);
        user.setAvatar_url(avatar_url);
        user.setDescription(description);
        return user;
    }
}
