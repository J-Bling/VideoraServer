package com.server.dao.video;

import com.server.dto.response.video.VideoClipsResponse;
import com.server.entity.video.VideoClip;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VideoClipDao {
    void insertVideoClip(VideoClip videoClip);
    List<VideoClipsResponse> findAllClipsByVideoIdWithQuality(@Param("videoId") Integer videoId,@Param("quality") boolean quality);
}
