package com.server.dao.video;

import com.server.dto.response.video.VideoClipsResponse;
import com.server.entity.video.VideoClip;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VideoClipDao {
    void insertVideoClip(VideoClip videoClip);
    List<VideoClipsResponse> findAllClipsByVideoIdWithQuality(@Param("videoId") Integer videoId,@Param("quality") boolean quality);

    @Select("select * from video_clip where video_id =#{id}")
    List<VideoClip> findAllByVideoId(@Param("id") Integer id);

    @Delete("delete from video_clip where video_id =#{id}")
    void deleteClipById(@Param("id") Long id);

    void insertVideoClipCopy(VideoClip videoClip);

    @Select("select * from video_clip_copy")
    List<VideoClip> findAllClipCopy();

    @Delete("delete from video_clip_copy")
    void deleteAllClipCopy();
}
