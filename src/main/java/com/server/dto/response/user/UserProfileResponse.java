package com.server.dto.response.user;

import com.server.entity.user.UserStats;
import com.server.entity.video.Video;

import java.util.List;

public class UserProfileResponse {
    private UserResponse userResponse;
    private UserStats userStats;
    private List<Video> likeVideos;//最近点赞
    private List<Video> coinVideos;//最近投币

    public UserResponse getUserResponse() {
        return userResponse;
    }

    public UserStats getUserStats() {
        return userStats;
    }

    public List<Video> getCoinVideos() {
        return coinVideos;
    }

    public List<Video> getLikeVideos() {
        return likeVideos;
    }

    public void setUserResponse(UserResponse userResponse) {
        this.userResponse = userResponse;
    }

    public void setCoinVideos(List<Video> coinVideos) {
        this.coinVideos = coinVideos;
    }

    public void setLikeVideos(List<Video> likeVideos) {
        this.likeVideos = likeVideos;
    }

    public void setUserStats(UserStats userStats) {
        this.userStats = userStats;
    }
}
