package com.server.service.videoservice.impl;


import com.server.dao.stats.VideoStatsDao;
import com.server.dao.video.VideoClipDao;
import com.server.dao.video.VideoDao;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.video.Video;
import com.server.entity.video.VideoClip;
import com.server.enums.ErrorCode;
import com.server.enums.ReviewCode;
import com.server.exception.ApiException;
import com.server.push.service.NotificationService;
import com.server.service.dynamic.DynamicService;
import com.server.service.stats.UserStatsService;
import com.server.service.videoservice.VideoEditService;
import com.server.service.videoservice.VideoFractionStatsService;
import com.server.service.videoservice.VideoService;
import com.server.util.ffmpeg.VideoCompressorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.server.enums.PresetProfile;

@Service
public class VideoUploadService implements VideoEditService {

    @Autowired private UserStatsService userStatsService;
    @Autowired private VideoStatsDao videoStatsDao;
    @Autowired private VideoDao videoDao;
    @Autowired private VideoClipDao videoClipDao;
    @Autowired private VideoCompressorUtil videoCompressorUtil;
    @Autowired private NotificationService notificationService;
    @Autowired private VideoService videoService;
    @Autowired private DynamicService dynamicService;
    @Autowired private VideoFractionStatsService videoFractionStatsService;


    @Value("${STATIC_FILE_URL}")
    private String STATIC_FILE_URL;
    @Value("${VIDEO_COVER_BASE_URL}")
    private  String VIDEO_COVER_BASE_URL;
    @Value("${VIDEO_FILE_BASE_URL}")
    private String VIDEO_FILE_BASE_URL;
    @Value("${VIDEO_COMPRESS_FILE_BASE_URL}")
    private String VIDEO_COMPRESS_FILE_BASE_URL;
    @Value("${MEDIA_PREDIX}")
    private String MEDIA_PREDIX;
    @Value("${MEDIA_COMPRESS_PREFIX}")
    private String MEDIA_COMPRESS_PREFIX;
    @Value("${IMAGE_COVER_PREFIX}")
    private String IMAGE_COVER_PREFIX;

    private final List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");
    List<String> videoFormats =
            Arrays.asList("mp4", "avi", "mkv", "mov", "webm", "flv", "mpeg", "mpg", "3gp", "ts", "m2ts", "vob");
    private static final ConcurrentLinkedQueue<VideoClip>COMPRESS_QUEUE =new ConcurrentLinkedQueue<>();
    private final Logger logger= LoggerFactory.getLogger(VideoUploadService.class);


    @Override
    public Integer createVideoInit(Video video, InputStream input, String OriginFilename) throws IOException {
        if(input==null || OriginFilename==null)
            throw new ApiException(ErrorCode.BAD_REQUEST);

        String fileExtension = OriginFilename.substring(OriginFilename.lastIndexOf(".") + 1).toLowerCase();
        if(!allowedExtensions.contains(fileExtension))
            throw new ApiException(ErrorCode.BAD_REQUEST);

        String suffix=UUID.randomUUID().toString()+System.currentTimeMillis()+OriginFilename;
        String filename=VIDEO_COVER_BASE_URL+suffix;
        String webUrl=IMAGE_COVER_PREFIX+suffix;

        File file=new File(filename);
        try(FileOutputStream outputStream=new FileOutputStream(file)){
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead=input.read(buffer))!=-1){
                outputStream.write(buffer,0,bytesRead);
            }
            outputStream.flush();
        }

        video.setCover_url(webUrl);
        video.setReview_status(ReviewCode.REVIEWING.getCode());
        this.videoDao.insertVideo(video);
        this.videoStatsDao.createVideoStatsTable(video.getId());
        this.userStatsService.CountVideo(video.getAuthor(), 1);
        return video.getId();
    }

    @Override
    public Integer saveVideoClip(VideoClip videoClip, InputStream videoFile,String OriginFilename) throws IOException {
        if(videoFile==null || OriginFilename==null || !videoClip.isVail())
            throw new ApiException(ErrorCode.BAD_REQUEST);

        String fileExtension = OriginFilename.substring(OriginFilename.lastIndexOf(".") + 1).toLowerCase();

        if(!videoFormats.contains(fileExtension))
            throw new ApiException(ErrorCode.BAD_REQUEST);

        String suffix=System.currentTimeMillis()+UUID.randomUUID().toString()+".mp4";
        String filename = VIDEO_FILE_BASE_URL+suffix;
        String webUrl=MEDIA_PREDIX+suffix;

        videoClip.setUrl(webUrl);
        videoClip.setFormat("mp4");
        videoClip.setQuality(true);

        File file=new File(filename);
        try(FileOutputStream outputStream = new FileOutputStream(file)){
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead=videoFile.read(buffer))!=-1){
                outputStream.write(buffer,0,bytesRead);
            }
            outputStream.flush();
        }

        this.videoClipDao.insertVideoClip(videoClip);

        videoClip.setUrl(filename);
        videoClip.setQuality(false);
        COMPRESS_QUEUE.offer(videoClip);

        return videoClip.getVideo_index()+1;
    }

    @Override
    public Integer saveVideoClip(VideoClip videoClip, MultipartFile videoFile) throws IOException {
        if(videoFile==null) throw new ApiException(ErrorCode.BAD_REQUEST);
        return this.saveVideoClip(videoClip,videoFile.getInputStream(),videoFile.getOriginalFilename());
    }

    @Override
    public void deleteVideoDataForUploadFail(Integer videoId, Integer authorId) {
        Video video =videoDao.findVideoById(videoId);
        if(video==null) return;

        String url = video.getCover_url();
        url = STATIC_FILE_URL+url;

        videoDao.deleteVideoById(videoId);
        userStatsService.CountVideo(authorId,-1);
        videoDao.deleteVideoStatsById(videoId);
        notificationService.auditingStatusNotification(authorId,videoId,false,video.getTitle());

        Object[] videoClips=COMPRESS_QUEUE.toArray();
        for(Object obj : videoClips){
            VideoClip clip = (VideoClip) obj;
            if(clip!=null){
                if(clip.getVideo_id().equals(videoId)){
                    COMPRESS_QUEUE.remove(obj);
                }
            }
        }

        try{
            new File(url).delete();
        }catch(Exception e){
            logger.error("remove file : {} , fail : {} ",url,e.getMessage(),e);
        }

       List<VideoClip> clips = videoClipDao.findAllByVideoId(videoId);
       if(clips==null || clips.isEmpty()) return;

       for(VideoClip clip : clips){
           videoClipDao.deleteClipById(clip.getId());
           String path = clip.getUrl();
           path = STATIC_FILE_URL+path;
           try{
               new File(path).delete();
           }catch(Exception e){
               logger.error("remove file : {} , fail : {} ",path,e.getMessage(),e);
           }
       }
    }

    @Scheduled(fixedRate = 75*1000)
    public void CompressVideoFromQueue() throws InterruptedException {
        if(COMPRESS_QUEUE.isEmpty()) return;
        List<VideoClip> videoClips=new ArrayList<>();

        VideoClip videoClip=COMPRESS_QUEUE.poll();
        while (videoClip!=null){
            videoClips.add(videoClip);
            videoClip=COMPRESS_QUEUE.poll();
        }

        if(videoClips.isEmpty()) return;

        for(VideoClip clip : videoClips){

            String suffix=System.currentTimeMillis()+UUID.randomUUID().toString()+".mp4";
            String filename=VIDEO_COMPRESS_FILE_BASE_URL+suffix;
            boolean stats= videoCompressorUtil.smartCompress(clip.getUrl(),filename,PresetProfile.BALANCE);
            VideoDataResponse video= videoService.getVideoResponseData(clip.getVideo_id(),null);
            if(!stats) {
                logger.error("文件压缩失败 ： 原文件名 ： {}", clip.getUrl());
         //      notificationService.auditingStatusNotification(video.getAuthorId(),video.getId(),false);
                this.deleteVideoDataForUploadFail(video.getId(),video.getAuthorId());
                continue;
            }

            if(video.getAuthorId()==null){
                if(video.getAuthor()!=null){
                    video.setAuthorId(video.getAuthor().getId());
                }
            }

            try {
                clip.setUrl(MEDIA_COMPRESS_PREFIX + suffix);
                this.videoClipDao.insertVideoClip(clip);
                this.videoDao.updateReviewStatus(clip.getVideo_id(), true);
                dynamicService.setVideoIdsInCache(video.getAuthorId(), clip.getVideo_id());
                notificationService.auditingStatusNotification(video.getAuthorId(), clip.getVideo_id(), true,null);
                notificationService.newDevelopmentToFunNotices(video.getAuthorId(), clip.getVideo_id());
                videoFractionStatsService.insertRank(video.getId(),video.getCategory());
            } catch (Exception e) {
                this.deleteVideoDataForUploadFail(video.getId(),video.getAuthorId());
                logger.error("压缩完成后 处理业务失败 失败原因 : {}",e.getMessage(),e);
            }
        }
    }
}
