package com.server.entity.cache.record;

import lombok.Data;

@Data
public class RecordUpdate {
    private int userId;
    private int targetId;
    private Boolean type;

    public RecordUpdate(){}
    public RecordUpdate(int userId,int targetId,Boolean type){
        this.userId=userId;
        this.targetId=targetId;
        this.type=type;
    }

}
