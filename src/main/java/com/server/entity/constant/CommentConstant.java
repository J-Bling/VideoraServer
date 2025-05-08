package com.server.entity.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CommentConstant {
    public static final String USER_STATS_TABLE="user_stats";
    public static final String VIDEO_STATS_TABLE="video_stats";
    public static final String VIDEO_COUNT="video_count";
    public static final String LIKE_COUNT="like_count";
    public static final String FOLLOWING_COUNT="following_count";
    public static final String FOLLOWER_COUNT="follower_count";
    public static final String COIN_BALANCE="coin_balance";
    public static final String FAVORITE_COUNT="favorite_count";
    public static final String VIEW_COUNT="view_count";
    public static final String COIN_COUNT="coin_count";
    public static final String SHARE_COUNT="share_count";
    public static final String BARRAGE_COUNT="barrage_count";

    public static final String COMMENT_TABLE="comment";
    public static final String COMMENT_REPLY_COUNT="reply_count";
    public static final String COMMENT_LIKE_COUNT="like_count";
    public static final String COMMENT_DISLIKE_COUNT="dislike_count";

    public static final String COMMENT_USER_ACTION_TABLE="comment_user_actions";
    public static final String COMMENT_USER_ACTION_TYPE="action_type";

    public static final Set<String> ALLOWED_USER_COLUMNS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    FOLLOWING_COUNT, FOLLOWER_COUNT,
                    COIN_BALANCE, FAVORITE_COUNT, VIDEO_COUNT
            )));

    public static final Set<String> ALLOWED_VIDEO_COLUMNS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    LIKE_COUNT, FAVORITE_COUNT, VIEW_COUNT,
                    COIN_COUNT, SHARE_COUNT, BARRAGE_COUNT
            )));

    public static final Set<String> ALLOWED_COMMENT_COLUMN=
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    COMMENT_REPLY_COUNT,COMMENT_DISLIKE_COUNT,COMMENT_LIKE_COUNT
            )));


    public static void validateColumn(String table, String column) {
        Set<String> allowedColumns = USER_STATS_TABLE.equals(table) ?
                ALLOWED_USER_COLUMNS : ALLOWED_VIDEO_COLUMNS;

        if (!allowedColumns.contains(column)) {
            throw new IllegalArgumentException("非法列名: " + column + " 对于表: " + table);
        }
    }


    public static void validateCommentColumn(String table,String column){
        if(COMMENT_TABLE.equals(table)){

            if(!ALLOWED_COMMENT_COLUMN.contains(column)){
                throw new IllegalArgumentException("非法列名: " + column + " 对于表: " + table);
            }
            return;
        }
        throw new IllegalArgumentException("非法表名: "+ table);
    }

    public static void validateCommentUserActionColumn(String table,String column){
        if(COMMENT_USER_ACTION_TABLE.equals(table)){

            if(!COMMENT_USER_ACTION_TYPE.equals(column)){
                throw new IllegalArgumentException("非法列名: " + column + " 对于表: " + table);
            }
        }

        throw new IllegalArgumentException("非法表名: "+ table);
    }
}
