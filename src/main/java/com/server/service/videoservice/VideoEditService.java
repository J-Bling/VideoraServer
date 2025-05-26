package com.server.service.videoservice;

import com.server.entity.video.Video;
import com.server.entity.video.VideoClip;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface VideoEditService {
    Integer createVideoInit(Video video, InputStream input,String OriginFilename) throws IOException;
    Integer saveVideoClip(VideoClip videoClip,InputStream videoFile,String OriginFilename) throws IOException;
    Integer saveVideoClip(VideoClip videoClip, MultipartFile videoFile) throws IOException;
    void deleteVideoDataForUploadFail(Integer videoId,Integer authorId);
    void deleteVideo(int videoId,int authorId);
}
