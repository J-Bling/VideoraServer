package com.server.dto.request.video;

import com.server.entity.video.VideoClip;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import lombok.Data;

@Data
public class VideoClipUploadRequest {
    private Integer video_id;
    private Integer video_index;
    private Double duration;
    private Integer width;
    private Integer height;
    private String format;

    public boolean isValue(){
        return video_id!=null && video_index!=null && duration!=null && width!=null && height!=null &&format!=null;
    }

    public VideoClip toEntity(){
        if(!this.isValue()) throw new ApiException(ErrorCode.BAD_REQUEST);
        VideoClip videoClip=new VideoClip();
        videoClip.setVideo_id(video_id);
        videoClip.setVideo_index(video_index);
        videoClip.setFormat(format);
        videoClip.setWidth(width);
        videoClip.setHeight(height);
        videoClip.setDuration(duration);
        return videoClip;
    }
}
