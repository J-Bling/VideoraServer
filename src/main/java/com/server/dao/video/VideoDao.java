package com.server.dao.video;

import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.video.Video;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.util.List;

public interface VideoDao {
    void insertVideo(Video video);
    Integer findVideoClipCount(@Param("videoId") Integer videoId);
    void updateReviewStatus(@Param("videoId") Integer videoId,@Param("stats") boolean stats);
    VideoDataResponse findVideoData(@Param("id") Integer id,@Nullable @Param("userId") Integer userId);
    List<VideoDataResponse> findVideos(@Param("offset") int offset,@Param("limit") int limit,@Param("isHot") boolean isHot);
    List<VideoDataResponse> findVideosByUserId(@Nullable @Param("userId") Integer userId,@Param("offset") int offset,@Param("limit") int limit,@Param("isHot") boolean isHot);//
    List<Integer> findVideosIdsByCategory(@Param("category") String category ,@Param("offset") int offset,@Param("limit") int limit,@Param("isHot") boolean isHot);


    List<VideoDataResponse> findVideoForDynamic(@Param("userId") Integer userId,@Param("lastCreated") Timestamp lastCreated,@Param("limit") int limit);

    /**
     * @return VideoDataResponse -> video + stats
     */
    List<VideoDataResponse> findVideoForDynamicByAuthor(@Param("authorId") Integer authorId,
                                                        @Nullable @Param("lastCreated") Timestamp lastCreated,
                                                        @Param("limit") int limit);

    @Delete("delete from video where id = #{id}")
    void deleteVideoById(@Param("id") Integer id);

    @Delete("delete from video_stats where video_id=#{id}")
    void  deleteVideoStatsById(@Param("id") Integer id);

    @Select("select * from video where id =#{id}")
    Video findVideoById(@Param("id") Integer id);

    List<VideoDataResponse> findVideo(@Param("limit") int limit,@Param("isHot") boolean isHot);

    List<Video> getRecentLikeVideo(@Param("userId") int userId,@Param("limit") int limit);
    List<Video> getRecentCoinVideo(@Param("userId") int userId,@Param("limit") int limit);

    List<Video> getContributeVideo(@Param("userId") int userId,@Param("offset") int offset, @Param("limit") int limit);
    List<VideoDataResponse> getCollection(@Param("userId") int userId,@Param("offset") int offset,@Param("limit") int limit);
}