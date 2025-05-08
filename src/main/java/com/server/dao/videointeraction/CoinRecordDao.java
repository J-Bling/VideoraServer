package com.server.dao.videointeraction;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoinRecordDao {

    @Select("select 1 from coin_record where video_id=#{videoId} and user_id=#{userId} limit 1")
    Integer existsCoined(@Param("videoId") Integer videoId,@Param("userId") Integer userId);
}
