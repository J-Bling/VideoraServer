package com.server.dto.response.video;

import lombok.Data;

@Data
public class VideoClipsResponse {
    private Integer video_index;
    private Integer duration;
    private String url;
}
