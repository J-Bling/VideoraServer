package com.server.service.stats;

import com.server.entity.video.VideoStats;

import java.util.List;

public interface VideoStatsService {
    VideoStats getVideoStats(Integer videoId) throws InterruptedException;
    void setVideoStatsOnCache(List<VideoStats> videoStats);
    void setVideoStatsOnCache(VideoStats videoStats);
    void CountView(Integer videoId,long count);
    void CountLike(Integer videoId,long count);
    void CountCoin(Integer videoId,long count);
    void CountFavorite(Integer videoId,long count);
    void CountShare(Integer videoId,long count);
    void CountBarrage(Integer videoId,long count);
}
