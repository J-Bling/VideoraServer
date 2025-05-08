package com.server.service.videoservice;


import com.server.dto.response.video.VideoClipsResponse;
import com.server.dto.response.video.VideoDataResponse;
import com.server.enums.VideoCategory;
import org.springframework.lang.Nullable;

import java.util.List;

public interface VideoService {
    List<VideoDataResponse> videoRecommendationsByRandom(@Nullable Integer userId, int offset) throws InterruptedException;//
    List<VideoDataResponse> videoRecommendationsByRandom(int offset) throws InterruptedException;
    List<VideoDataResponse> videoRecommendationsByCategory(Integer userId, VideoCategory category,int offset) throws InterruptedException;
    List<VideoDataResponse> videoRecommendationsByCategory(VideoCategory category, int offset) throws InterruptedException;
    List<VideoClipsResponse> getVideoClipUrl(Integer videoId,int offset,boolean quality);
    VideoDataResponse getVideoResponseData(Integer videoId,@Nullable Integer userId) throws InterruptedException;
    List<VideoDataResponse> getMaxHotVideoData();
}
