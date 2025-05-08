package com.server.entity.videointeraction;


public class FavoritesRecord {
    private int id;
    private int user_id;
    private int video_id;
    private long created;


    public FavoritesRecord(){}
    public FavoritesRecord(int user_id,int video_idl){
        this.user_id=user_id;
        this.video_id=video_idl;
        this.created=System.currentTimeMillis()/1000;
    }
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getVideo_id() {
        return video_id;
    }

    public void setVideo_id(int video_id) {
        this.video_id = video_id;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }
}
