package com.server.entity.cache;

import com.server.dto.response.user.UserResponse;
import lombok.Data;

@Data
public class UserResponseCache {
    private Integer id;
    private String nickname;
    private Boolean gender;
    private String avatar_url;
    private String description;

    public UserResponseCache(){}
    public UserResponseCache(UserResponse userResponse){
        this.id=userResponse.getId();
        this.nickname=userResponse.getNickname();
        this.gender=userResponse.getGender();
        this.avatar_url=userResponse.getAvatar_url();
        this.description=userResponse.getDescription();
    }
}
