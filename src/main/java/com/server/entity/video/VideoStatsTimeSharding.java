package com.server.entity.video;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VideoStatsTimeSharding {
    private Integer id;
    private int video_id;
    private LocalDate stat_date;
    private int stat_type;
    private int view_count;
    private int like_count;
    private int favorite_count;

    public VideoStatsTimeSharding(){}
    public VideoStatsTimeSharding(VideoStats videoStats){
        this.video_id=videoStats.getVideo_id();
        this.stat_date=LocalDate.now();
        this.stat_type=1;
        this.view_count=videoStats.getView_count();
        this.like_count=videoStats.getLike_count();
        this.favorite_count=videoStats.getFavorite_count();
    }
}
