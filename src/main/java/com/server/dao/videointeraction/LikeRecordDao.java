package com.server.dao.videointeraction;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface LikeRecordDao {
    @Select("select 1 from like_record where video_id=#{videoId} and user_id=#{userId} limit 1")
    Boolean findIsLike(@Param("userId") Integer userId,@Param("videoId") Integer videoId);
}
