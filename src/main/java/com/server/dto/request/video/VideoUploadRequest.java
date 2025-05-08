package com.server.dto.request.video;

import com.server.entity.video.Video;
import com.server.enums.VideoCategory;
import lombok.Data;

@Data
public class VideoUploadRequest {
    private String title;
    private String description;
    private String category;
    private Double file_size;
    private Integer duration;
    private Integer width;
    private Integer height;
    private String format;
    private Integer clips_count;

    public boolean isVail(){
        return title!=null
                && description!=null
                && VideoCategory.isVailName(category)
                && file_size!=null
                && duration!=null
                && width!=null
                && height!=null
                && format!=null
                && clips_count>0;
    }

    public Video toEntity(){
        Video video =new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setCategory(category);
        video.setFile_size(file_size);
        video.setDuration(duration);
        video.setWidth(width);
        video.setHeight(height);
        video.setFormat(format);
        video.setClips_count(clips_count);
        return video;
    }
    public VideoUploadRequest(){}
    public VideoUploadRequest(String title,String description,String category,
                              Double file_size,Integer duration,Integer width,
                              Integer height,String format,Integer clips_count)
    {
        this.title=title;
        this.description=description;
        this.category=category;
        this.file_size=file_size;
        this.duration=duration;
        this.width=width;
        this.height=height;
        this.format=format;
        this.clips_count=clips_count;
    }
}
