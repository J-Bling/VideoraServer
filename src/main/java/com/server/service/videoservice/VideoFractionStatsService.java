package com.server.service.videoservice;

import java.util.Set;

public interface VideoFractionStatsService {
    void increaseFraction(String field,double count);
    Long sizeForFraction();
    Set<Object> rangeFraction(int offset, int limit);
}
