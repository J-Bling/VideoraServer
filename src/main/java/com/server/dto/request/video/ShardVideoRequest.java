package com.server.dto.request.video;

import lombok.Data;

@Data
public class ShardVideoRequest {
    private Integer segment_index;
    private Integer video_id;
    private Integer duration;
}
