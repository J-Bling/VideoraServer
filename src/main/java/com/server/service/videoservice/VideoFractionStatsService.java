package com.server.service.videoservice;

import com.server.dto.response.video.VideoDataResponse;

import java.util.List;

public interface VideoFractionStatsService {
    void init();
    void insertRank(Integer videoId,String category);
    void insertRank(List<VideoDataResponse> videoDataResponse);
    List<String> getVideoId(String category,long start,long end);
    List<String> getVideoId(long start, long end);
    void increaseFraction(String field,double count);
}
