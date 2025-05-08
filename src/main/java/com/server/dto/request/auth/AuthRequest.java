package com.server.dto.request.auth;

import com.server.entity.user.User;

public class AuthRequest {
    private String nickname;
    private String phone;
    private String email;
    private String code;
    private String password;

    public AuthRequest(){}
    public AuthRequest(String nickname,String phone,String email,String password){
        this.nickname=nickname;this.phone=phone;this.email=email;this.password=password;
    }
    public AuthRequest(String phone,String email,String password){
        this.phone=phone;this.email=email;this.password=password;
    }
    public User toEntity(){
        User user= new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword(password);
        return user;
    }
    public boolean Vail(){
        return !(nickname==null || password==null || (phone==null && email==null));
    }

    public String getCode() {
        return code;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
