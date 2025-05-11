package com.server.entity.video;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Video {
    private Integer id;
    private String title;
    private String description;
    private Integer author;
    private String category;
    private Integer file_size;//大小mb
    private Integer duration;//时长
    private Integer width;
    private Integer height;
    private String format;
    private String cover_url;
    private Integer review_status; // 0:待审核 1:通过 2:不通过
    private String review_fail_reason;
    private Integer clips_count;
    private LocalDate created;
    private LocalDate updated;
}
