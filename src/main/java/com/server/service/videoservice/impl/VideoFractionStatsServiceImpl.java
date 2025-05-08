package com.server.service.videoservice.impl;

import com.server.entity.constant.RedisKeyConstant;
import com.server.service.videoservice.VideoFractionStatsService;
import com.server.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class VideoFractionStatsServiceImpl implements VideoFractionStatsService {

    @Autowired private RedisUtil redis;

    @Override
    public void increaseFraction(String field, double count) {
        redis.zIncrBy(RedisKeyConstant.VIDEO_RANKING_KEY,field,count);
    }

    @Override
    public Long sizeForFraction(){
        Long count=redis.zCard(RedisKeyConstant.VIDEO_RANKING_KEY);
        return count!=null ? count : 0;
    }


    @Override
    public Set<Object> rangeFraction(int offset,int limit){
        if(limit==0) return null;
        Set<Object> videoIds = new HashSet<>();
        if(offset%2==0){
            videoIds.addAll(redis.zRange(RedisKeyConstant.VIDEO_RANKING_KEY, offset / 2, (offset / 2) + (limit / 2)));
            videoIds.addAll(redis.zRange(RedisKeyConstant.VIDEO_RANKING_KEY, -(offset / 2), -(offset / 2 + limit / 2)));
        }else {
            videoIds.addAll(redis.zRange(RedisKeyConstant.VIDEO_RANKING_KEY,(offset+1)/2,(offset+1)/2+limit/2));
            videoIds.addAll(redis.zRange(RedisKeyConstant.VIDEO_RANKING_KEY,-((offset+1)/2-1),-((offset+1)/2-1+limit/2)));
        }

        return videoIds;
    }
}
