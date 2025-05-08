package com.server.entity.cache.video;

import com.server.dto.response.user.UserResponse;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.video.VideoStats;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class VideoResponseCache {
    private Integer id;
    private String title;
    private String description;
    private String category;
    private Integer duration;//时长
    private Integer width;
    private Integer height;
    private String format;
    private String cover_url;
    private Integer clips_count;
    private Timestamp created;
    private Timestamp updated;
    private Integer authorId;
    private VideoStats videoStats; //不会是null
    private UserResponse author; // 不会是null

    public static VideoResponseCache formVideoDataResponse(VideoDataResponse dataResponse){
        VideoResponseCache responseCache=new VideoResponseCache(dataResponse);
        UserResponse userResponse=dataResponse.getAuthor();
        if(userResponse!=null) {
            UserResponse response =
                    new UserResponse(
                            userResponse.getId(),userResponse.getNickname(),
                            userResponse.getGender(),userResponse.getAvatar_url(),
                            userResponse.getDescription());
            responseCache.setAuthor(response);
        }
        return responseCache;
    }

    public VideoResponseCache(){}
    public VideoResponseCache(VideoDataResponse videoDataResponse){
        id=videoDataResponse.getId();
        title=videoDataResponse.getTitle();
        description=videoDataResponse.getDescription();
        category=videoDataResponse.getCategory();
        duration=videoDataResponse.getDuration();
        width=videoDataResponse.getWidth();
        height=videoDataResponse.getHeight();
        cover_url=videoDataResponse.getCover_url();
        clips_count=videoDataResponse.getClips_count();
        created=videoDataResponse.getCreated();
        updated=videoDataResponse.getUpdated();
        authorId=videoDataResponse.getAuthorId();
        videoStats=videoDataResponse.getVideoStats();

    }
    public VideoDataResponse toEntity(){
        VideoDataResponse videoDataResponse = new VideoDataResponse();
        videoDataResponse.setId(id);
        videoDataResponse.setTitle(title);
        videoDataResponse.setDescription(description);
        videoDataResponse.setCategory(category);
        videoDataResponse.setDuration(duration);
        videoDataResponse.setWidth(width);
        videoDataResponse.setHeight(height);
        videoDataResponse.setFormat(format);
        videoDataResponse.setCover_url(cover_url);
        videoDataResponse.setClips_count(clips_count);
        videoDataResponse.setCreated(created);
        videoDataResponse.setUpdated(updated);
        videoDataResponse.setAuthorId(authorId);
        return videoDataResponse;
    }
}
