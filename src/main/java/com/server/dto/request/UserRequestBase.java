package com.server.dto.request;

import lombok.Data;

@Data
public class UserRequestBase {
    protected String account;
    protected boolean accountType;
    protected String password;
    protected Integer scene;
}
