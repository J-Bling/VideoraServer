package com.server.service.stats;

import com.server.entity.constant.RedisKeyConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stats {
    protected static String USER_STATS(Integer userId){
        return RedisKeyConstant.USER_STATS+userId;
    };
    protected static String VIDEO_STATS(Integer videoId){
        return RedisKeyConstant.VIDEO_STATS+videoId;
    };

    protected static final String USER_STATS_LOCK= RedisKeyConstant.USER_STATS_LOCK;
    protected static final long EXPIRED =RedisKeyConstant.EXPIRED;
    protected static final String USER_STATS_UPDATE_KEY=RedisKeyConstant.USER_STATS_UPDATE_KEY;
    protected static final String VIDEO_STATS_LOCK=RedisKeyConstant.VIDEO_STATS_LOCK;
    protected static final String VIDEO_STATS_UPDATE_KEY=RedisKeyConstant.VIDEO_STATS_UPDATE_KEY;
    protected final Logger logger = LoggerFactory.getLogger(Stats.class);
    protected static final String NULL=RedisKeyConstant.NULL;
    protected static final String LOCK_VALUE=RedisKeyConstant.LOCK_VALUE;
}
