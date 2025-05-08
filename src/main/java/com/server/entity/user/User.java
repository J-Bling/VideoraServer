package com.server.entity.user;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class User {
    private Integer id;
    private String nickname;
    private String phone;
    private String email;
    @JsonIgnoreProperties
    private String salt;
    @JsonIgnoreProperties
    private String password;
    private Boolean gender;//0男 1女
    private String avatar_url;
    private String description;
    private Boolean locked;
    private Boolean admin;
    private Timestamp created;

    public User(){}
    public User(String nickname,String password,String avatar_url,Boolean gender){
        this.nickname=nickname;this.password=password;this.avatar_url=avatar_url;this.gender=gender;
    }
}
