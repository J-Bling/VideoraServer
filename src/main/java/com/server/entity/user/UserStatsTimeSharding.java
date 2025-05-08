package com.server.entity.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserStatsTimeSharding {
    private Integer id;
    private Integer user_id;
    private Integer video_count;
    private Integer like_count;
    private Integer following_count;
    private Integer follower_count;
    private LocalDate created;
    private Integer time_type;

    public UserStatsTimeSharding(){}

    public UserStatsTimeSharding(UserStats userStats){
        this.user_id=userStats.getUser_id();
        this.video_count=userStats.getVideo_count();
        this.like_count=userStats.getLike_count();
        this.following_count=userStats.getFollowing_count();
        this.follower_count=userStats.getFollower_count();
        this.created=LocalDate.now();
        this.time_type=1;
    }
}
