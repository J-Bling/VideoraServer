package com.server.dao.videointeraction;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FavoritesRecordDao {

    @Select("select 1 from favorites_record where user_id=#{userId} and video_id=#{videoId} limit 1")
    Boolean findFavType(@Param("userId") Integer userid,@Param("videoId") Integer videoId);
}
