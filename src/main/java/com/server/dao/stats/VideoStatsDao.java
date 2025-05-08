package com.server.dao.stats;

import com.server.entity.video.VideoStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VideoStatsDao {
    @Insert("insert into video_stats (video_id) values (#{videoId})")
    void createVideoStatsTable(@Param("videoId") Integer videoId);

    @Select("select * from video_stats where video_id=#{video_id}")
    VideoStats findVideoStats(@Param("video_id") int video_id);
}
