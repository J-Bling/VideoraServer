package com.server.dto.request.comment;

import lombok.Data;

@Data
public class CommentRequest {
    private Integer video_id;
    private Integer root_id;
    private Integer parent_id;
    private String content;
    private Integer author_id;
}
