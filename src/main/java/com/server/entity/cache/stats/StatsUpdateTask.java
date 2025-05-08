package com.server.entity.cache.stats;

public class StatsUpdateTask {
    private String column;
    private int count;
    private int targetId;

    public StatsUpdateTask(){}
    public StatsUpdateTask(int targetId,String column,int count){
        this.column=column;
        this.count=count;
        this.targetId=targetId;
    }
    public String getColumn() {
        return column;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
}
