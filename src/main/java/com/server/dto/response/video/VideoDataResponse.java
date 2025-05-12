package com.server.dto.response.video;

import com.server.dto.response.user.UserResponse;
import com.server.dto.response.video.record.VideoRecordForUser;
import com.server.entity.video.VideoStats;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Schema(description = "视频数据响应对象，包含视频详细信息、作者信息、统计数据和用户观看记录")
public class VideoDataResponse {
    private Integer id;

    private String title;

    private String description;

    private String category;

    private Integer duration;

    private Boolean review_status;

    private Integer width;

    private Integer height;

    private String format;

    private String cover_url;

    private Integer clips_count;

    private Timestamp created;

    private Timestamp updated;

    private Integer authorId;

    private VideoRecordForUser videoRecordForUser;

    private VideoStats videoStats;

    private UserResponse author;
}