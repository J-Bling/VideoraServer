package com.server.entity.video;

import lombok.Data;

@Data
public class VideoStats {
    private Integer video_id;
    private int view_count;//播放
    private int like_count;//点赞
    private int coin_count;//投币
    private int favorite_count;//收藏
    private int share_count;//分享数
    private int barrage_count;//弹幕数

    public VideoStats(){}
    public VideoStats(Integer video_id){
        this.video_id=video_id;
        view_count=
        like_count=
        coin_count=
        favorite_count=
        share_count= barrage_count=0;
    }
    public VideoStats(Integer video_id,int view_count,
                      int like_count,int coin_count,
                      int favorite_count,int share_count,
                      int barrage_count)
    {
        this.video_id=video_id;
        this.view_count=view_count;
        this.like_count=like_count;
        this.coin_count=coin_count;
        this.favorite_count= favorite_count;
        this.share_count=share_count;
        this.barrage_count=barrage_count;
    }
}
