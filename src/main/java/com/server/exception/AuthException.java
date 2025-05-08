package com.server.exception;

import com.server.enums.AuthErrorCode;


public class AuthException extends RuntimeException {
    private int code;
    private String message;

  // 直接传递枚举值
    public AuthException(AuthErrorCode errorCode) {
          code=errorCode.getCode();
          message=errorCode.getMessage();
    }

    @Override
    public String getMessage() {
        return message;
    }

    // 通过错误码构造
    public AuthException(int code) {
        this(AuthErrorCode.fromStatusCode(code));
    }

    // 可选：快速抛出常见错误的静态方法
    public static AuthException emailAlreadyExists() {
        return new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }

    public static AuthException incorrectPassword() {
      return new AuthException(AuthErrorCode.INCORRECT_PASSWORD);
    }
}
