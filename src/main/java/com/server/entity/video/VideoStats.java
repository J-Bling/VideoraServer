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
    public VideoStats(Integer video_id,String view_count,
                      Object like_count,Object coin_count,
                      Object favorite_count,Object share_count,
                      Object barrage_count)
    {
        this.video_id=video_id;
        this.view_count=Integer.parseInt(view_count);
        this.like_count=Integer.parseInt(like_count.toString());
        this.coin_count=Integer.parseInt(coin_count.toString());
        this.favorite_count= Integer.parseInt(favorite_count.toString());
        this.share_count=Integer.parseInt(share_count.toString());
        this.barrage_count=Integer.parseInt(barrage_count.toString());
    }
}
