package com.server.dto.response.video.record;

import lombok.Data;

@Data
public class VideoRecordForUser {
    private Boolean hadCoin;
    private Boolean hadFavorites;
    private Boolean hadLike;

    public VideoRecordForUser(){}
    public VideoRecordForUser(Boolean hadCoin,Boolean hadFavorites,Boolean hadLike){
        this.hadCoin=hadCoin;this.hadFavorites=hadFavorites;this.hadLike=hadLike;
    }
}
