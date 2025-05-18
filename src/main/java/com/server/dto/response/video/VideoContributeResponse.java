package com.server.dto.response.video;

import com.server.entity.video.Video;
import com.server.entity.video.VideoStats;

public class VideoContributeResponse {
    private Video video;
    private VideoStats videoStats;

    public Video getVideo() {
        return video;
    }

    public VideoStats getVideoStats() {
        return videoStats;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public void setVideoStats(VideoStats videoStats) {
        this.videoStats = videoStats;
    }
}
