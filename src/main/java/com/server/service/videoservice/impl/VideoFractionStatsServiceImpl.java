package com.server.service.videoservice.impl;

import com.server.dao.video.VideoDao;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.video.VideoStats;
import com.server.service.videoservice.VideoFractionStatsService;
import com.server.util.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class VideoFractionStatsServiceImpl implements VideoFractionStatsService {

    @Autowired private RedisUtil redis;
    @Autowired private VideoDao videoDao;

    private final int MAX_VIDEO_RANK_SIZE=500;
    private final Logger logger = LoggerFactory.getLogger(VideoFractionStatsServiceImpl.class);
    private final String VIDEO_RANKING_KEY = RedisKeyConstant.VIDEO_RANKING_KEY;
    private String VIDEO_RANK_FOR_CATEGORY(String category){
        return VIDEO_RANKING_KEY+category;
    }


    private Double computedFraction(VideoStats videoStats){
        return videoStats.getView_count()
                + videoStats.getLike_count() *2.0
                + videoStats.getFavorite_count() *3.0
                + videoStats.getCoin_count() *4.0;
    }

    @PostConstruct
    @Override
    public void init(){
        try{
            Map<Integer,Double> fraction = new HashMap<>();
            Set<ZSetOperations.TypedTuple<Object>> fractionSet = new HashSet<>();

            List<VideoDataResponse> videoDataResponses = videoDao.findVideoByCategory(MAX_VIDEO_RANK_SIZE,true);
            if(videoDataResponses==null || videoDataResponses.isEmpty()) return ;
            videoDataResponses.addAll(videoDao.findVideoByCategory(MAX_VIDEO_RANK_SIZE,false));

            Map<String, Set<ZSetOperations.TypedTuple<Object>>> categoryForVideoId=new HashMap<>();

            for(VideoDataResponse videoDataResponse : videoDataResponses){
                Integer videoId =videoDataResponse.getVideoStats().getVideo_id();
                if(fraction.get(videoId)!=null){
                    fraction.put(videoId,0.0);
                    Double f = computedFraction(videoDataResponse.getVideoStats());
                    ZSetOperations.TypedTuple<Object> typedTuple=new DefaultTypedTuple<>(videoId,f);

                    fractionSet.add(typedTuple);
                    Set<ZSetOperations.TypedTuple<Object>> typedTuples = categoryForVideoId.computeIfAbsent(
                            VIDEO_RANK_FOR_CATEGORY(videoDataResponse.getCategory()),
                            k->new HashSet<>()
                    );
                    typedTuples.add(typedTuple);
                }
            }

            try {
                redis.getRedisTemplate().opsForZSet().add(VIDEO_RANKING_KEY, fractionSet);
            }catch (Exception e){
                logger.error("insert redis zSet on VIDEO_RANKING_KEY fail : {}",e.getMessage());
            }

            try{
                for(Map.Entry<String, Set<ZSetOperations.TypedTuple<Object>>> typedTupleEntry : categoryForVideoId.entrySet()){
                    redis.getRedisTemplate().opsForZSet().add(typedTupleEntry.getKey(),typedTupleEntry.getValue());
                }
            }catch (Exception e){
                logger.error("insert redis zSet on VIDEO_RANK_FOR_CATEGORY fail :{}",e.getMessage());
            }


        }catch (Exception e){
            logger.error("VideoFractionStatsServiceImpl.init fail : {}",e.getMessage());
        }
    }

    @Override
    public void insertRank(Integer videoId,String category){
        try{
            redis.zAdd(VIDEO_RANKING_KEY,1000.0,videoId.toString());
            redis.zAdd(VIDEO_RANK_FOR_CATEGORY(category),1000.0,videoId.toString());
        }catch (Exception e){
            logger.error("VideoFractionStatsServiceImpl.insertRank fail : {}",e.getMessage());
        }
    }



    @Override
    public List<String> getVideoId(long start,long end){
        try{
            Set<Object> fractions=redis.zRange(VIDEO_RANKING_KEY,start,end);

            if(fractions==null || fractions.isEmpty()) return null;
            List<String> array=new ArrayList<>();
            for(Object fraction : fractions){
                array.add(fraction.toString());
            }

            return array;
        }catch (Exception e){
            logger.error(e.getMessage());
            return  null;
        }
    }

    @Override
    public List<String> getVideoId(String category,long start,long end){
        try{
            Set<Object> fractions=redis.zRange(VIDEO_RANK_FOR_CATEGORY(category),start,end);
            if(fractions==null || fractions.isEmpty()) return null;

            List<String> array=new ArrayList<>();
            for(Object fraction : fractions){
                array.add(fraction.toString());
            }

            return array;
        }catch (Exception e){
            logger.error(e.getMessage());
            return  null;
        }
    }

    @Override
    public void increaseFraction(String field, double count) {
        redis.zIncrBy(VIDEO_RANKING_KEY,field,count);
    }
}