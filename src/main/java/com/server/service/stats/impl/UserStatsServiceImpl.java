package com.server.service.stats.impl;

import com.server.dao.stats.UserStatsDao;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.user.UserStats;
import com.server.service.stats.Stats;
import com.server.service.stats.UserStatsService;
import com.server.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;


@Service
public class UserStatsServiceImpl extends Stats implements UserStatsService {

    @Autowired
    private RedisUtil redis;

    @Autowired
    private UserStatsDao userStatsDao;


    @Override
    public UserStats getUserStats(Integer userId) throws InterruptedException {
        if(userId==null) return null;
        String videoCount=(String)redis.hGet(USER_STATS(userId),RedisKeyConstant.USER_VIDEO_COUNT);
        if(NULL.equals(videoCount)) return null;

        if(videoCount!=null){
            try {
                return new UserStats(
                        userId,
                        videoCount,
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_LIKE_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_FOLLOWING_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_FOLLOWER_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_COIN_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_FAVORITE_COUNT)
                );
            }catch (Exception e){
                logger.error("query1 or conversion fail  reason is  {}",e.getMessage() ,e);
                return null;
            }
        }

        Boolean isLock=redis.setIfAbsent(USER_STATS_LOCK+userId,LOCK_VALUE,EXPIRED);
        if(isLock==null) return null;

        if(isLock) {
            UserStats userStats = userStatsDao.findUserStats(userId);

            Map<String, Object> user_stats = getStringObjectMap(userStats);
            redis.hmSet(USER_STATS(userId),user_stats, RedisKeyConstant.CLEAN_CACHE_SPACED);

            return userStats;

        }else {
            Thread.sleep(EXPIRED); //线程等待

            try {

                return new UserStats(
                        userId,
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_VIDEO_COUNT).toString(),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_LIKE_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_FOLLOWING_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_FOLLOWER_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_COIN_COUNT),
                        redis.hGet(USER_STATS(userId), RedisKeyConstant.USER_FAVORITE_COUNT)
                );
            }catch (Exception e){
                logger.error("query2 or conversion fail  reason is  {}",e.getMessage() ,e);
                return null;
            }
        }
    }

    private static Map<String, Object> getStringObjectMap(UserStats userStats) {
        Map<String,Object> user_stats=new HashMap<>();
        if(userStats !=null){
            user_stats.put(RedisKeyConstant.USER_VIDEO_COUNT,""+ userStats.getVideo_count());
            user_stats.put(RedisKeyConstant.USER_LIKE_COUNT,""+ userStats.getLike_count());
            user_stats.put(RedisKeyConstant.USER_FOLLOWING_COUNT,""+ userStats.getFollowing_count());
            user_stats.put(RedisKeyConstant.USER_FOLLOWER_COUNT,""+ userStats.getFollower_count());
            user_stats.put(RedisKeyConstant.USER_COIN_COUNT,""+ userStats.getCoin_balance());
            user_stats.put(RedisKeyConstant.USER_FAVORITE_COUNT,""+ userStats.getFavorite_count());

        }else{
            user_stats.put(RedisKeyConstant.USER_VIDEO_COUNT,NULL);
            user_stats.put(RedisKeyConstant.USER_LIKE_COUNT,NULL);
            user_stats.put(RedisKeyConstant.USER_FOLLOWING_COUNT,NULL);
            user_stats.put(RedisKeyConstant.USER_FOLLOWER_COUNT,NULL);
            user_stats.put(RedisKeyConstant.USER_COIN_COUNT,NULL);
            user_stats.put(RedisKeyConstant.USER_FAVORITE_COUNT,NULL);

        }
        return user_stats;
    }

    private void pushUpdate(String field,long count){
        redis.hInCrBy(USER_STATS_UPDATE_KEY,field,count);
    }

    private void updateCache(String key,String field,long count,Integer userId){
        redis.hInCrBy(key,field,count);
        this.pushUpdate(field+userId,count);
    }

    @Override
    public void CountVideo(Integer userId, long count) {
        updateCache(USER_STATS(userId),RedisKeyConstant.USER_VIDEO_COUNT,count,userId);
    }

    @Override
    public void CountLike(Integer userId, long count) {
        if(count==0) return;
        updateCache(USER_STATS(userId),RedisKeyConstant.USER_LIKE_COUNT,count,userId);
    }

    @Override
    public void CountFollowing(Integer userId, long count) {
        if(count==0) return;
        updateCache(USER_STATS(userId),RedisKeyConstant.USER_FOLLOWING_COUNT,count,userId);
    }

    @Override
    public void CountFollower(Integer userId, long count) {
        if(count==0) return;
        updateCache(USER_STATS(userId),RedisKeyConstant.USER_FOLLOWER_COUNT,count,userId);
    }

    @Override
    public void CountCoin(Integer userId, long count) {
        updateCache(USER_STATS(userId),RedisKeyConstant.USER_COIN_COUNT,count,userId);
    }

    @Override
    public void CountFavorite(Integer userId, long count) {
        if(count==0) return;
        updateCache(USER_STATS(userId),RedisKeyConstant.USER_FAVORITE_COUNT,count,userId);
    }

    @Override
    public void setUserStatsOnCache(UserStats stats) {
        if(stats==null) return;

        Map<String, Object> user_stats = getStringObjectMap(stats);
        redis.hmSet(USER_STATS(stats.getUser_id()),user_stats, RedisKeyConstant.CLEAN_CACHE_SPACED);
    }

    @Override
    public void recordOnline(String userId) {
        try {
            String lastOnlineTimeStr =(String) redis.hGet(RedisKeyConstant.USER_ONLINE_RECORD, userId);
            if (lastOnlineTimeStr == null) {
                redis.hSet(RedisKeyConstant.USER_ONLINE_RECORD, userId, "" + System.currentTimeMillis());
                CountCoin(Integer.parseInt(userId), 1);
                return;
            }
            long lastOnlineTime = Long.parseLong(lastOnlineTimeStr);
            LocalDate localDate = Instant.ofEpochMilli(lastOnlineTime).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate today = LocalDate.now();

            if (!today.isEqual(localDate)) {
                redis.hSet(RedisKeyConstant.USER_ONLINE_RECORD, userId, "" + System.currentTimeMillis());
                CountCoin(Integer.parseInt(userId), 1);
            }
        } catch (Exception e) {
            logger.error("发生错误 {} ",e.getMessage());
        }
    }
}
