package com.server.entity.video;

import lombok.Data;

@Data
public class VideoClip {
    private Long id;
    private Integer video_id;
    private Integer video_index;
    private Double duration;
    private String url;
    private Boolean quality;//视频质量 原/压缩
    private Integer width;
    private Integer height;
    private String format;

    public boolean isVail(){
        return video_id!=null && duration!=null && video_index!=null;
    }
    public VideoClip(){}
    public VideoClip(Integer video_id,Integer video_index,Double duration){
        this.video_id=video_id;
        this.video_index=video_index;
        this.duration=duration;
    }
}
