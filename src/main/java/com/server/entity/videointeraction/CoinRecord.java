package com.server.entity.videointeraction;


public class CoinRecord {
    private Integer id;
    private int user_id;
    private int video_id;
    private Integer amount;

    public CoinRecord(){}
    public CoinRecord(int user_id,int video_id){
        this.user_id=user_id;
        this.video_id=video_id;
        this.amount=1;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public void setVideo_id(int video_id) {
        this.video_id = video_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getVideo_id() {
        return video_id;
    }

    public Integer getId() {
        return id;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}

