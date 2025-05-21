package com.server.service.stats.impl;

import com.server.dao.stats.VideoStatsDao;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.video.VideoStats;
import com.server.service.stats.Stats;
import com.server.service.stats.VideoStatsService;
import com.server.service.videoservice.VideoFractionStatsService;
import com.server.service.videoservice.impl.VideoFractionStatsServiceImpl;
import com.server.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VideoStatsServiceImpl extends Stats implements VideoStatsService {

    @Autowired
    private RedisUtil redis;

    @Autowired
    private VideoStatsDao videoStatsDao;

    @Autowired
    private VideoFractionStatsService videoFractionStatsService;

    private Map<String, Object> getStringObjectMap(VideoStats videoStats) {
        Map<String,Object> videoStatsMap=new HashMap<>();
        videoStatsMap.put(RedisKeyConstant.VIDEO_VIEW_COUNT,""+ videoStats.getView_count());
        videoStatsMap.put(RedisKeyConstant.VIDEO_LIKE_COUNT,""+ videoStats.getLike_count());
        videoStatsMap.put(RedisKeyConstant.VIDEO_FAVORITE_COUNT,""+ videoStats.getFavorite_count());
        videoStatsMap.put(RedisKeyConstant.VIDEO_COIN_COUNT,""+ videoStats.getCoin_count());
        videoStatsMap.put(RedisKeyConstant.VIDEO_SHARE_COUNT,""+ videoStats.getShare_count());
        videoStatsMap.put(RedisKeyConstant.VIDEO_BARRAGE_COUNT,""+ videoStats.getBarrage_count());
        return videoStatsMap;
    }


    @Override
    public VideoStats getVideoStats(Integer videoId) throws InterruptedException {
        String viewCount=(String) redis.hGet(VIDEO_STATS(videoId), RedisKeyConstant.VIDEO_VIEW_COUNT);

        if(NULL.equals(viewCount)){
            return null;
        }

        if(viewCount!=null){
            try{
                return new VideoStats(
                        videoId,
                        Integer.parseInt(viewCount),
                        Integer.parseInt((String)redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_LIKE_COUNT)),
                        Integer.parseInt((String) redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_COIN_COUNT)),
                        Integer.parseInt((String) (redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_FAVORITE_COUNT))),
                        Integer.parseInt((String) redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_SHARE_COUNT)),
                        Integer.parseInt((String) redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_BARRAGE_COUNT))
                );

            }catch (Exception e){
                logger.error("query1 or conversion fail reason is {}",e.getMessage(),e);
                return null;
            }
        }

        Boolean isLock = redis.setIfAbsent(VIDEO_STATS_LOCK,LOCK_VALUE,EXPIRED);
        if(isLock==null) return null;

        if(isLock){
            VideoStats videoStats = videoStatsDao.findVideoStats(videoId);
            if(videoStats !=null){
                Map<String, Object> videoStatsMap = getStringObjectMap(videoStats);
                redis.hmSet(VIDEO_STATS(videoId),videoStatsMap,RedisKeyConstant.CLEAN_CACHE_SPACED);

            }else{
                Map<String,Object> videoStatsMap=new HashMap<>();
                videoStatsMap.put(RedisKeyConstant.VIDEO_VIEW_COUNT,NULL);
                videoStatsMap.put(RedisKeyConstant.VIDEO_LIKE_COUNT,NULL);
                videoStatsMap.put(RedisKeyConstant.VIDEO_FAVORITE_COUNT,NULL);
                videoStatsMap.put(RedisKeyConstant.VIDEO_COIN_COUNT,NULL);
                videoStatsMap.put(RedisKeyConstant.VIDEO_SHARE_COUNT,NULL);
                videoStatsMap.put(RedisKeyConstant.VIDEO_BARRAGE_COUNT,NULL);
                redis.hmSet(VIDEO_STATS(videoId),videoStatsMap,RedisKeyConstant.CLEAN_CACHE_SPACED);
            }

            return videoStats;
        }else {
            Thread.sleep(EXPIRED);

            try{
                return new VideoStats(
                        videoId,
                        Integer.parseInt((String)redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_VIEW_COUNT)),
                        Integer.parseInt((String)redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_LIKE_COUNT)),
                        Integer.parseInt((String) redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_COIN_COUNT)),
                        Integer.parseInt((String) (redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_FAVORITE_COUNT))),
                        Integer.parseInt((String) redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_SHARE_COUNT)),
                        Integer.parseInt((String) redis.hGet(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_BARRAGE_COUNT))
                );

            }catch (Exception e){
                logger.error("query or conversion fail reason is {}",e.getMessage(),e);
                return null;
            }
        }
    }


    @Override
    public void setVideoStatsOnCache(VideoStats videoStats) {
        if(videoStats==null) return;
        Map<String, Object> videoStatsMap = getStringObjectMap(videoStats);
        String key =VIDEO_STATS(videoStats.getVideo_id());
        redis.hmSet(key,videoStatsMap,RedisKeyConstant.CLEAN_CACHE_SPACED);
    }

    @Override
    public void setVideoStatsOnCache(List<VideoStats> videoStats) {
        for(VideoStats stats : videoStats){
            setVideoStatsOnCache(stats);
        }
    }

    @Override
    public void CountView(Integer videoId, long count) {
        if(count<=0) return;
        redis.hInCrBy(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_VIEW_COUNT,count);
        redis.hInCrBy(VIDEO_STATS_UPDATE_KEY,RedisKeyConstant.VIDEO_VIEW_COUNT+videoId,count);
        this.videoFractionStatsService.increaseFraction(""+videoId,count);
    }

    @Override
    public void CountLike(Integer videoId, long count) {
        if(count==0) return;
        redis.hInCrBy(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_LIKE_COUNT,count);
        redis.hInCrBy(VIDEO_STATS_UPDATE_KEY,RedisKeyConstant.VIDEO_LIKE_COUNT+videoId,count);
        this.videoFractionStatsService.increaseFraction(""+videoId,count);
    }

    @Override
    public void CountCoin(Integer videoId, long count) {
        if(count==0) return;
        redis.hInCrBy(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_COIN_COUNT,count);
        redis.hInCrBy(VIDEO_STATS_UPDATE_KEY,RedisKeyConstant.VIDEO_COIN_COUNT+videoId,count);
        this.videoFractionStatsService.increaseFraction(""+videoId,count);
    }

    @Override
    public void CountFavorite(Integer videoId, long count) {
        if(count==0) return;
        redis.hInCrBy(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_FAVORITE_COUNT,count);
        redis.hInCrBy(VIDEO_STATS_UPDATE_KEY,RedisKeyConstant.VIDEO_FAVORITE_COUNT+videoId,count);
        this.videoFractionStatsService.increaseFraction(""+videoId,count);
    }

    @Override
    public void CountShare(Integer videoId, long count) {
        if(count==0) return;
        redis.hInCrBy(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_SHARE_COUNT,count);
        redis.hInCrBy(VIDEO_STATS_UPDATE_KEY,RedisKeyConstant.VIDEO_SHARE_COUNT+videoId,count);
        this.videoFractionStatsService.increaseFraction(""+videoId,count);
    }

    @Override
    public void CountBarrage(Integer videoId, long count) {
        if(count==0) return;
        redis.hInCrBy(VIDEO_STATS(videoId),RedisKeyConstant.VIDEO_BARRAGE_COUNT,count);
        redis.hInCrBy(VIDEO_STATS_UPDATE_KEY,RedisKeyConstant.VIDEO_BARRAGE_COUNT+videoId,count);
        this.videoFractionStatsService.increaseFraction(""+videoId,count);
    }
}
