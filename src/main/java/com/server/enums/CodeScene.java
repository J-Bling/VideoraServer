package com.server.enums;

import com.server.exception.ApiException;

public enum CodeScene {
    LOGIN(1,"login"),
    REGISTER(2,"register"),
    RESET(3,"reset");

    private final int code;
    private final String scene;

    CodeScene(int code,String scene){
        this.code=code;this.scene=scene;
    }

    public static int fromCode(int code){
        for(CodeScene codeScene:values()){
            if(codeScene.getCode()==code)
                return code;
        }
        throw new ApiException(ErrorCode.BAD_REQUEST);
    }

    public int getCode() {
        return code;
    }

    public String getScene() {
        return scene;
    }
}
