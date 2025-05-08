package com.server.push.dto.response;

import com.server.enums.WSStatusCode;

public class MessageErrorCode {
    private Integer code;
    private String description;

    public MessageErrorCode(){}
    public MessageErrorCode(WSStatusCode statusCode){
        this();
        this.code=statusCode.getCode();
        this.description=statusCode.getDescription();
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
