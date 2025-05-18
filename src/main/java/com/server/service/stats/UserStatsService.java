package com.server.service.stats;

import com.server.entity.user.UserStats;

public interface UserStatsService {
    UserStats getUserStats(Integer userId) throws InterruptedException;
    void CountVideo(Integer userId,long count);
    void CountLike(Integer userId,long count);
    void CountFollowing(Integer userId,long count);
    void CountFollower(Integer userId,long count);
    void CountCoin(Integer userId,long count);
    void CountFavorite(Integer userId,long count);
    void setUserStatsOnCache(UserStats stats);

    void recordOnline(String userId);
}
