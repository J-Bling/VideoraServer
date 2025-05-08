package com.server.entity.comment;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CommentUserActions {
    private Integer id;
    private Integer comment_id;
    private Integer user_id;
    private Boolean action_type;//false 踩  true赞
    private Timestamp created;
}
