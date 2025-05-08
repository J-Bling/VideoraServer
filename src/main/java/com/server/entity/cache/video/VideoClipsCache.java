package com.server.entity.cache.video;

import lombok.Data;

@Data
public class VideoClipsCache {
    private Integer duration;
    private Integer width;
    private Integer height;

    public VideoClipsCache(){}
    public VideoClipsCache(Integer duration,Integer width,Integer height){
        this.duration=duration;
        this.width=width;
        this.height=height;
    }
}
