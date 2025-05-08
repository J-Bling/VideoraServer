package com.server.enums;

import com.server.exception.ApiException;

public enum StatsTimeType {
    DAY(1,"日"),
    MONTH(2,"月"),
    YEAR(3,"年");

    private final int code;
    private final String description;

    StatsTimeType(int code,String description){
        this.code=code;this.description=description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StatsTimeType formCode(int code){
        for(StatsTimeType statsTimeType : values()){
            if(statsTimeType.getCode()==code){
                return statsTimeType;
            }
        }
        throw new ApiException(ErrorCode.BAD_REQUEST);
    }
}
