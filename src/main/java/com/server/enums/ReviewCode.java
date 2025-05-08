package com.server.enums;

import com.server.exception.ApiException;

public enum ReviewCode {
    REVIEWING(0,"审核中"),
    REVIEW_SUCCEED(1,"审核通过"),
    REVIEW_FAIL(2,"审核失败");

    private int code;
    private String description;

    ReviewCode(int code, String description) {
        this.code=code;
        this.description=description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ReviewCode formCode(int code){
        for(ReviewCode reviewCode :values()){
            if(reviewCode.code==code)
                return reviewCode;
        }
        throw new ApiException(ErrorCode.BAD_REQUEST);
    }
}
