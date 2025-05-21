package com.server.service.videoservice.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.dao.video.VideoClipDao;
import com.server.dao.video.VideoDao;
import com.server.dto.response.user.UserResponse;
import com.server.dto.response.video.VideoClipsResponse;
import com.server.dto.response.video.VideoDataResponse;
import com.server.dto.response.video.record.VideoRecordForUser;
import com.server.entity.cache.video.VideoResponseCache;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.user.UserRelation;
import com.server.entity.video.VideoStats;
import com.server.enums.VideoCategory;
import com.server.service.interaction.InteractionService;
import com.server.service.stats.UserStatsService;
import com.server.service.stats.VideoStatsService;
import com.server.service.userservice.UserDataService;
import com.server.service.videoservice.VideoFractionStatsService;
import com.server.service.videoservice.VideoService;
import com.server.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    @Autowired private VideoDao videoDao;
    @Autowired private VideoClipDao videoClipDao;
    @Autowired private VideoStatsService videoStatsService;
    @Autowired private UserStatsService userStatsService;
    @Autowired private InteractionService interactionService;
    @Autowired private UserDataService userDataService;
    @Autowired private RedisUtil redis;
    @Autowired private VideoFractionStatsService videoFractionStatsService;


    private final ObjectMapper mapper = new ObjectMapper();
    private static final int LIMIT=16;
    private static final int CLIPS_COUNT_LIMIT=20;
    private static final int MAX_HOT_VIDEO_SIZE=5;
    private final Logger logger= LoggerFactory.getLogger(VideoServiceImpl.class);

    private String VIDEO_DATA_KEY(Integer videoId){
        return RedisKeyConstant.VIDEO_DATA_KEY+videoId;
    }
    private String VIDEO_CLIPS_LOW_HASH_KEY(Integer videoId){
        return RedisKeyConstant.VIDEO_CLIPS_DATA_KEY+"l:"+videoId;
    }
    private String VIDEO_CLIPS_HEIGHT_HASH_KEY(Integer videoId){
        return RedisKeyConstant.VIDEO_CLIPS_DATA_KEY+"h:"+videoId;
    }
    private String VIDEO_DATA_LOCK(Integer videoId){
        return RedisKeyConstant.FIND_VIDEO_DATA_LOCK+videoId;
    }


    private void setVideoDataInCache(VideoDataResponse videoDataResponse,Integer userId){
        if(videoDataResponse==null) return;
        try{
            String data=mapper.writeValueAsString(VideoResponseCache.formVideoDataResponse(videoDataResponse));
            redis.set(VIDEO_DATA_KEY(videoDataResponse.getId()),data,RedisKeyConstant.CLEAN_CACHE_SPACED);
            userDataService.setUserResponseOnCache(videoDataResponse.getAuthor());
            VideoStats videoStats=videoDataResponse.getVideoStats();
            videoStats.setVideo_id(videoDataResponse.getId());
            videoStatsService.setVideoStatsOnCache(videoStats);
            interactionService.setRecordOnCache(videoDataResponse.getVideoRecordForUser(),userId, videoDataResponse.getId());
            interactionService.setRelationOnCache(videoDataResponse.getAuthor().getUserRelation());
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }


    private VideoRecordForUser findRecord(Integer videoId,Integer userId) throws InterruptedException {
        return new VideoRecordForUser(
                this.interactionService.hadCoin(userId,videoId),
                this.interactionService.findFavoriteRecord(userId,videoId),
                this.interactionService.findLikeRecord(userId,videoId));
    }

    private VideoDataResponse findVideoDataOnDB(Integer videoId,Integer userId){
        VideoDataResponse videoResponseData= this.videoDao.findVideoData(videoId, userId);
        if(videoResponseData==null) {
            redis.set(VIDEO_DATA_KEY(videoId),RedisKeyConstant.NULL,RedisKeyConstant.CLEAN_CACHE_SPACED);
            return null;
        }

        if(userId!=null){
            UserResponse userResponse=videoResponseData.getAuthor();
            if(userResponse!=null &&  userId.equals(userResponse.getId())) userResponse.setUserRelation(null);
        }
        this.setVideoDataInCache(videoResponseData,userId);
        return videoResponseData;
    }


    private VideoDataResponse findVideoDataOnCache(Integer videoId,Integer userId) throws InterruptedException {
        String string=redis.get(VIDEO_DATA_KEY(videoId));

        if(string!=null){
            if(RedisKeyConstant.NULL.equals(string)){
                return null;
            }
            VideoDataResponse data =null;
            try {
                 data= mapper.readValue(string, mapper.constructType(VideoDataResponse.class));
            }catch (JacksonException jacksonException){
                return null;
            }

            VideoStats videoStats= this.videoStatsService.getVideoStats(videoId);
            data.setVideoStats(videoStats);

            UserResponse userResponse=data.getAuthor();
            if(userResponse==null){
                userResponse=this.userDataService.getUserDataWithStats(data.getAuthorId());
                data.setAuthor(userResponse);
            }

            if(userId!=null){
                if(userResponse!=null ) {
                    userResponse.setUserRelation(userId.equals(userResponse.getId()) ? null :
                            new UserRelation(this.interactionService.findRelation(userId, userResponse.getId())));
                    userResponse.setUserStats(this.userStatsService.getUserStats(userResponse.getId()));
                }

                data.setVideoRecordForUser(this.findRecord(videoId,userId));
            }

            return data;
        }else {
            Boolean isLock=redis.setIfAbsent(VIDEO_DATA_LOCK(videoId),RedisKeyConstant.LOCK_VALUE,RedisKeyConstant.EXPIRED);
            if(isLock!=null && !isLock) {
                Thread.sleep(RedisKeyConstant.EXPIRED);
                return this.findVideoDataOnCache(videoId,userId);
            }
            return this.findVideoDataOnDB(videoId,userId);
        }
    }


    private void setVideoClipDataInCache(Integer videoId,List<VideoClipsResponse> clipsResponses,boolean quality){
        if(videoId==null || clipsResponses==null) return;
        Map<String, Object> videoClips = new HashMap<>();
        for (VideoClipsResponse clipsResponse : clipsResponses) {
            try{
                String data=mapper.writeValueAsString(clipsResponse);
                videoClips.put("" + clipsResponse.getVideo_index(), data);
            }catch(Exception e){
                logger.error("",e);
            }
        }
        if (quality) {
            this.redis.hmSet(VIDEO_CLIPS_HEIGHT_HASH_KEY(videoId), videoClips,RedisKeyConstant.CLEAN_CACHE_SPACED);
        }else {
            this.redis.hmSet(VIDEO_CLIPS_LOW_HASH_KEY(videoId),videoClips,RedisKeyConstant.CLEAN_CACHE_SPACED);
        }
    }

    private List<VideoClipsResponse> findVideoClipsOnCache(Integer videoId,int offset,boolean quality){
        if(redis.hLen(quality ? VIDEO_CLIPS_HEIGHT_HASH_KEY(videoId) : VIDEO_CLIPS_LOW_HASH_KEY(videoId))<1){
            List<VideoClipsResponse> clipsResponses=this.videoClipDao.findAllClipsByVideoIdWithQuality(videoId,quality);
            this.setVideoClipDataInCache(videoId,clipsResponses,quality);
            if(clipsResponses==null || clipsResponses.size()<=offset) return null;
            return clipsResponses.subList(offset, Math.min(CLIPS_COUNT_LIMIT, clipsResponses.size()));
        }

        Set<Object> clips=new HashSet<>();
        for(int i=offset>0 ? offset :1 ;i<=CLIPS_COUNT_LIMIT+offset;i++){
            clips.add(""+i);
        }
        List<Object> videoClips= this.redis.hmGet(quality ? VIDEO_CLIPS_HEIGHT_HASH_KEY(videoId) : VIDEO_CLIPS_LOW_HASH_KEY(videoId),clips);
        List<VideoClipsResponse> videoClipsResponses=new ArrayList<>();
        for(Object videoClip : videoClips){
            if(videoClip!=null){
                VideoClipsResponse videoClipsResponse = this.redis.deserialize((String) videoClip,mapper.constructType(VideoClipsResponse.class));
                if(videoClipsResponse!=null) videoClipsResponses.add(videoClipsResponse);
            }
        }

        return videoClipsResponses;
    }

    private List<VideoDataResponse> getRecommentdationsOnDb(Integer userId,int offset){
        List<VideoDataResponse> videoDataResponses = videoDao.findVideos(offset,LIMIT-1,true);
        if(videoDataResponses==null || videoDataResponses.isEmpty()) return videoDataResponses;


        for(VideoDataResponse response : videoDataResponses){
            setVideoDataInCache(response,null);
        }
        return videoDataResponses;
    }

    private List<VideoDataResponse> getRecommentByCategoryOnDb(Integer userId,String categoryName,int offset) throws InterruptedException {
        List<Integer> videoIds = videoDao.findVideosIdsByCategory(categoryName,offset,LIMIT-1,true);
        if(videoIds==null || videoIds.isEmpty()) return null;

        List<VideoDataResponse> videoDataResponses = new ArrayList<>();
        for(Integer id : videoIds){
            videoDataResponses.add(findVideoDataOnCache(id,null));
        }

        return videoDataResponses;
    }

    @Override
    public List<VideoDataResponse> videoRecommendationsByRandom(Integer userId, int offset) throws InterruptedException {
        List<String> videoIds=videoFractionStatsService.getVideoId(offset,offset+LIMIT);
        if(videoIds==null || videoIds.isEmpty()){
            List<VideoDataResponse> videoDataResponses = getRecommentdationsOnDb(userId,offset);
            videoFractionStatsService.insertRank(videoDataResponses);
            return videoDataResponses;
        }

        List<VideoDataResponse> videoDataResponses =new ArrayList<>();

        for(String id : videoIds){
            VideoDataResponse dataResponse= this.findVideoDataOnCache(Integer.parseInt(id),userId);
            if(dataResponse!=null){
                videoDataResponses.add(dataResponse);
            }
        }

        return videoDataResponses;
    }

    public List<VideoDataResponse> videoRecommendationsByRandom(int offset) throws InterruptedException{
        return videoRecommendationsByRandom(null,offset);
    }

    @Override
    public List<VideoDataResponse> videoRecommendationsByCategory(Integer userId, VideoCategory category, int offset) throws InterruptedException {
         List<String> videoIds=videoFractionStatsService.getVideoId(category.getName(),offset,offset+LIMIT);
         if(videoIds==null || videoIds.isEmpty()){
             List<VideoDataResponse> videoDataResponses = getRecommentByCategoryOnDb(userId,category.getName(),offset);
             videoFractionStatsService.insertRank(videoDataResponses);
             return videoDataResponses;
         }

        List<VideoDataResponse> videoDataResponses =new ArrayList<>();

        for(String id : videoIds){
            VideoDataResponse dataResponse= this.findVideoDataOnCache(Integer.parseInt(id),userId);
            if(dataResponse!=null){
                videoDataResponses.add(dataResponse);
            }
        }

        return videoDataResponses;
    }

    public List<VideoDataResponse> videoRecommendationsByCategory(VideoCategory category, int offset) throws InterruptedException{
        return videoRecommendationsByCategory(null,category,offset);
    }

    @Override
    public VideoDataResponse getVideoResponseData(Integer videoId,Integer userId) throws InterruptedException {
        return this.findVideoDataOnCache(videoId,userId);
    }


    @Override
    public List<VideoClipsResponse> getVideoClipUrl(Integer videoId, int offset,boolean quality) {
        return this.findVideoClipsOnCache(videoId,offset,quality);
    }

    @Override
    public List<VideoDataResponse> getMaxHotVideoData() {
        try{
            List<String> videoIds= videoFractionStatsService.getVideoId(0,MAX_HOT_VIDEO_SIZE);
            if(videoIds==null || videoIds.isEmpty()) return null;

            List<VideoDataResponse> videoDataResponses=new ArrayList<>();
            for(String videoId : videoIds){
                VideoDataResponse dataResponse= this.findVideoDataOnCache(Integer.parseInt(videoId),null);
                if(dataResponse!=null) videoDataResponses.add(dataResponse);
            }

            return videoDataResponses;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("getMaxHotVideoData fail reason is ",e.getMessage());
            return null;
        }
    }
}
