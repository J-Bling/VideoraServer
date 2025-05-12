package com.server.service.videoservice;

import java.util.List;
import java.util.Set;

public interface VideoFractionStatsService {
    void init();
    void insertRank(Integer videoId,String category);
    List<String> getVideoId(String category,long start,long end);
    List<String> getVideoId(long start, long end);
    void increaseFraction(String field,double count);
    Long sizeForFraction();
}
