package com.server.enums;

import com.server.exception.ApiException;

public enum AccountType {
    EMAIL(true,1),
    PHONE(false,0);

    private boolean type;
    private Integer code;

    AccountType(boolean type,Integer code){
        this.type=type;
        this.code=code;
    }

    public void isVailFoeCode(Integer code){
        if(!EMAIL.code.equals(code) || !PHONE.code.equals(code))
            throw new ApiException(ErrorCode.BAD_REQUEST);
    }


    public void setType(boolean type) {
        this.type = type;
    }

    public boolean isType() {
        return type;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
