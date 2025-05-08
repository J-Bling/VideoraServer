package com.server.dto.response.user;

import lombok.Data;

@Data
public class UserStatsResponse {
    UserResponse user;
    protected Integer video_count;//发布视频数
    protected Integer following_count;//关注数
    protected Integer follower_count;//粉丝数
    protected Integer coin_balance;//硬币剩余
    protected Integer view_count;//视频总播放量
    public UserStatsResponse(){}
}
