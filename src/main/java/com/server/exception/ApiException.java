package com.server.exception;

import com.server.enums.ErrorCode;

public class ApiException extends RuntimeException{
    private final ErrorCode errorCode;

    public  ApiException(int code){
        this.errorCode=ErrorCode.fromStatusCode(code);
    }

    public ApiException(ErrorCode errorCode){this.errorCode=errorCode;}

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
