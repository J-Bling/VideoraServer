package com.server.entity.constant;

public class RedisKeyConstant {
    public static final long EXPIRED =1000L;//锁生命周期
    public static final long CLEAN_CACHE_SPACED=36L*60*60*1000;//数据缓存36小时生命周期
    public static final long RANK_CACHE_LIFE_CYCLE=72L*60*60*1000;//排行榜生命周期
    public static final long USER_ACTIVATE_FILE=48L*60*60*1000; //未读消息存放2天


    public static final String USER_STATS="user_stats:";
    public static final String VIDEO_STATS="video_stats:";
    public static final String COIN_RECORD_HASH_KEY="coin_record:";
    public static final String USER_RELATION_HASH_KEY="user_relation:";
    public static final String LIKE_HASH_KEY="like_record:";
    public static final String FAVORITE_RECORD_HASH_KEY="favorites_record:";

    public static final String USER_STATS_UPDATE_KEY="user_stats:update";
    public static final String VIDEO_STATS_UPDATE_KEY="video_stats:update";
    public static final String UPDATE_USER_RELATION_KEY="user_relation:update";
    public static final String UPDATE_LIKE_HASH_KEY="like_record:update";
    public static final String UPDATE_FAVORITE_KEY="favorites_record:update";
    public static final String UPDATE_COIN_KEY="coin_record:update";

    public static final String USER_STATS_LOCK= "user_stats:lock:";
    public static final String VIDEO_STATS_LOCK="video_stats:lock:";
    public static final String VIDEO_LIKE_LOCK="video_like:lock:";
    public static final String USER_RELATION_LOCK="user_relation:lock:";
    public static final String FAVORITE_RECORD_LOCK="favorite_record:lock:";
    public static final String COIN_RECORD_LOCK="coin_record:lock:";
    public static final String COIN_COUNT_LOCK="coin_count:lock:";

    public static final String VIDEO_VIEW_COUNT="view_count:";
    public static final String VIDEO_LIKE_COUNT="like_count:";
    public static final String VIDEO_COIN_COUNT="coin_count:";
    public static final String VIDEO_FAVORITE_COUNT="favorite_count:";
    public static final String VIDEO_SHARE_COUNT="share_count:";
    public static final String VIDEO_BARRAGE_COUNT="barrage_count:";
    public static final String USER_VIDEO_COUNT="video_count:";
    public static final String USER_LIKE_COUNT="like_count:";
    public static final String USER_FOLLOWING_COUNT="following_count:";
    public static final String USER_FOLLOWER_COUNT="follower_count:";
    public static final String USER_COIN_COUNT="coin_balance:";
    public static final String USER_FAVORITE_COUNT="favorite_count:";
    public static final String USER_ONLINE_RECORD="user_online_record";


    public static final String VIDEO_RANKING_KEY="video_rank:";
    public static final String VIDEO_DATA_KEY="video_data:";
    public static final String VIDEO_CLIPS_DATA_KEY="video_clips_data:";
    public static final String USER_DATA_KEY="user_data:";
    public static final String FIND_VIDEO_DATA_LOCK="find_video_data_lock:";
    public static final String COIN_LOCK="coin_lock:";
    public static final String USER_LOCK="user_data_lock:";

    public static final String HISTORY_MESSAGE_LIST_KEY="history_list_messages:";
    public static final String MESSAGE_UPDATE_LIST_KEY="message_list_update";
    public static final String UNREAD_MESSAGE_LIST_KEY="unread_list_messages:";//userId
    public static final String INSERT_MESSAGE_LIST_KEY="message_insert_list";

    public static final String INSERT_MSG_FOR_MESSAGE_LIST_KEY="msg_message_insert_list";
    public static final String HISTORY_MESSAGES_LIST_KEY="messages_history:";

    public static final String COMMENT_KEY="comment:";

    public static final String COMMENT_VIDEO_OFFSET_LOCK="comment_v_o_lock:";
    public static final String COMMENT_REPLY_OFFSET_LOCK="comment_r_o_lock:";
    public static final String COMMENT_INSERT_LIST_KEY="comment_insert";
    public static final String COMMENT_SCORE_RANK="comment_rank:";
    public static final String COMMENT_FOR_VIDEO_LIST_KEY="comment_v_list:";
    public static final String COMMENT_FOR_ROOT_LIST_KEY="comment_r_list:";
    public static final String COMMENT_ACTION_KEY="comment_action:";
    public static final long COMMENT_ALL_LIFT=64*60*60*1000;
    public static final String COMMENT_STATS_HASH_KEY="comment_stats";
    public static final String COMMENT_ACTION_INSERT_LIST_KEY="comment_a_i_list";
    public static final String COMMENT_ACTION_DELETE_LIST_KEY="comment_a_d_list";
    public static final String COMMENT_COUNT_UPDATE_HASH_KEY="comment_count_update";
    public static final String COMMENT_STRING_KEY="comment_s:";
    public static final String COMMENT_ACTION_CLOC="c_a_lock:";



    public static final String NULL="NULL";
    public static final String NULL_RECORD="";
    public static final String NO="0";
    public static final String IS="1";
    public static final String LOCK_VALUE="1";
    public static final String INTERVAL_NUMBER=":";
}

