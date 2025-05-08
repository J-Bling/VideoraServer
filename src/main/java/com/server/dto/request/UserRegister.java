package com.server.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true) // 显式包含父类字段
public class UserRegister extends UserRequestBase{
    protected String nickname;
    protected Boolean gender;//0男1女
    protected String code;//验证码
    protected String password1;
}
