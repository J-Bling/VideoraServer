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
    @Schema(description = "视频ID", example = "1")
    private Integer id;

    @Schema(description = "视频标题", example = "如何学习Java")
    private String title;

    @Schema(description = "视频描述", example = "这是一个关于如何学习Java的视频")
    private String description;

    @Schema(description = "视频分类", example = "教育")
    private String category;

    @Schema(description = "视频时长（秒）", example = "300")
    private Integer duration;

    @Schema(description = "视频宽度（像素）", example = "1920")
    private Integer width;

    @Schema(description = "视频高度（像素）", example = "1080")
    private Integer height;

    @Schema(description = "视频格式（如 mp4、webm）", example = "mp4")
    private String format;

    @Schema(description = "视频封面URL")
    private String cover_url;

    @Schema(description = "视频分片数量", example = "5")
    private Integer clips_count;

    @Schema(description = "视频创建时间", example = "2024-01-01T12:00:00")
    private Timestamp created;

    @Schema(description = "视频更新时间", example = "2024-01-02T12:00:00")
    private Timestamp updated;

    @Schema(description = "视频作者ID", example = "101")
    private Integer authorId;

    @Schema(description = "用户观看记录（游客或非详情页可能为null）", required = false)
    private VideoRecordForUser videoRecordForUser;

    @Schema(description = "视频统计数据（不会为null）", required = true)
    private VideoStats videoStats;

    @Schema(description = "视频作者信息（不会为null）", required = true)
    private UserResponse author;
}