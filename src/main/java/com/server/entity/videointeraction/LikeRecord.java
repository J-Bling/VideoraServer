package com.server.entity.videointeraction;

public class LikeRecord {
    private Integer id;
    private int video_id;
    private int user_id;
    public LikeRecord(){}
    public LikeRecord(int user_id,int video_id){
        this.user_id=user_id;
        this.video_id=video_id;
    }

    public Integer getId() {
        return id;
    }

    public void setVideo_id(int video_id) {
        this.video_id = video_id;
    }

    public int getVideo_id() {
        return video_id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}
