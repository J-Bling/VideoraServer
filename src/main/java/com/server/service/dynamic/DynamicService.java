package com.server.service.dynamic;


import com.server.dto.response.video.VideoDataResponse;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.util.List;

public interface DynamicService {
    void setVideoIdsInCache(Integer authorId,Integer videoIds);
    List<VideoDataResponse> findVideoByAuthor(Integer userId,Timestamp lastCreated);
    List<VideoDataResponse> findVideoDataByAuthorId(Integer authorId, @Nullable Timestamp lastCreated, int offset) throws InterruptedException;
}
