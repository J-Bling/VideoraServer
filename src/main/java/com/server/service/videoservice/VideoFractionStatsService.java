package com.server.service.videoservice;

import java.util.List;

public interface VideoFractionStatsService {
    void init();
    void insertRank(Integer videoId,String category);
    List<String> getVideoId(String category,long start,long end);
    List<String> getVideoId(long start, long end);
    void increaseFraction(String field,double count);
}
