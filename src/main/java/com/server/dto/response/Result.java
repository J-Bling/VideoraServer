package com.server.dto.response;

import com.server.enums.ErrorCode;
import org.springframework.http.ResponseEntity;


public class Result {
    private String message;
    private Object data;


    public static final Result result=new Result();

    public Result(){}
    public Result(Object data){this.data=data;}

    public static ResponseEntity<Result> Ok(Object data){
        return ResponseEntity.status(200).body(result.setMessage("ok").setData(data));
    }

    public static ResponseEntity<Result> ErrorResult(ErrorCode errorCode,Object data){
        return ResponseEntity.status(errorCode.getStatusCode()).
                body(result.setMessage(errorCode.getStandardMessage()).setData(data));
    }


    public Result setData(Object data) {
        this.data = data;
        return this;
    }

    public Result setMessage(String message) {
        this.message = message;
        return this;
    }


    public Object getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
