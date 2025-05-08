package com.server.enums;

public enum AuthErrorCode {
    INVALID_REQUEST(4000, "请求参数非法"),
    INTERNAL_SERVER_ERROR(5000, "服务器内部错误"),

    // 注册相关错误
    EMAIL_ALREADY_EXISTS(4001, "该帐号已被注册"),
    INVALID_EMAIL_FORMAT(4003, "邮箱格式不正确"),
    INVALID_PHONE_FORMAT(4004, "手机号格式不正确"),
    WEAK_PASSWORD(4005, "密码强度不足"),

    // 登录相关错误
    USER_NOT_FOUND(4010, "用户不存在"),
    INCORRECT_PASSWORD(4011, "密码错误"),
    ACCOUNT_LOCKED(4012, "账号已被锁定"),
    LOGIN_EXPIRED(4013, "登录已过期");

    private final int code;
    private final String message;

    AuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static AuthErrorCode fromStatusCode(int code) {
        for (AuthErrorCode error : AuthErrorCode.values()) {
            if (error.code == code) {
                return error;
            }
        }
        throw new IllegalArgumentException("无效的错误码: " + code);
    }
}
