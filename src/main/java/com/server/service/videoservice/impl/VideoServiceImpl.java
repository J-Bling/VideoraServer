package com.server.service.videoservice.impl;

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


    private static final int LIMIT=10;
    private static final int CLIPS_COUNT_LIMIT=20;
    private static final int MAX_HOT_VIDEO_SIZE=5;
    private final Logger logger= LoggerFactory.getLogger(VideoServiceImpl.class);

    private String VIDEO_LOCK_BY_OFFSET(int offset){
        return RedisKeyConstant.VIDEO_LOCK_BY_OFFSET+offset;
    }
    private String VIDEO_DATA_KEY(Integer videoId){
        return RedisKeyConstant.VIDEO_DATA_KEY+videoId;
    }
    private String VIDEO_CLIPS_LOW_HASH_KEY(Integer videoId){
        return RedisKeyConstant.VIDEO_CLIPS_DATA_KEY+"l:"+videoId;
    }
    private String VIDEO_CLIPS_HEIGHT_HASH_KEY(Integer videoId){
        return RedisKeyConstant.VIDEO_CLIPS_DATA_KEY+"h:"+videoId;
    }


    private void setVideoDataInCache(VideoDataResponse videoDataResponse,Integer userId){
        if(videoDataResponse==null) return;
        ObjectMapper mapper =new ObjectMapper();
        try{
            String data=mapper.writeValueAsString(VideoResponseCache.formVideoDataResponse(videoDataResponse));
            redis.set(VIDEO_DATA_KEY(videoDataResponse.getId()),data,RedisKeyConstant.CLEAN_CACHE_SPACED);
            userDataService.setUserResponseOnCache(videoDataResponse.getAuthor());
            VideoStats videoStats=videoDataResponse.getVideoStats();
            videoStats.setVideo_id(videoDataResponse.getId());
            videoStatsService.setVideoStatsOnCache(videoStats);
            interactionService.setRecordOnCache(videoDataResponse.getVideoRecordForUser(),userId, videoDataResponse.getId());
            interactionService.setRelationOnCache(videoDataResponse.getAuthor().getUserRelation());
            this.updateFraction(videoDataResponse.getId(),videoDataResponse.getVideoStats());
        }catch (Exception e){
            logger.error("",e);
        }
    }

    private VideoDataResponse getVideoDataInCache(Integer videoId){
        String data=redis.get(VIDEO_DATA_KEY(videoId));
        if(data==null) return null;
        ObjectMapper mapper =new ObjectMapper();
        return redis.deserialize(data,mapper.constructType(VideoDataResponse.class));
    }


    public void updateFraction(Integer videoId,VideoStats videoStats){
        //更新缓存分数
        if(videoStats!=null){
            long fraction=videoStats.getView_count()+videoStats.getLike_count()+videoStats.getFavorite_count()
                    +videoStats.getCoin_count()+RedisKeyConstant.VIDEO_INIT_FRACTION;
            this.redis.zAdd(RedisKeyConstant.VIDEO_RANKING_KEY,fraction,RedisKeyConstant.VIDEO_RANKING_FIELD+videoId);
            redis.hSet(RedisKeyConstant.VIDEO_RANK_LIFE_HASH_KEY,RedisKeyConstant.VIDEO_RANKING_FIELD+videoId,""+System.currentTimeMillis());
        }
    }



    private void updateVideoCache(List<VideoDataResponse> videoDataResponses,Integer userId){
        if(videoDataResponses==null || videoDataResponses.isEmpty()) return;

        for(VideoDataResponse videoDataResponse : videoDataResponses){
            this.setVideoDataInCache(videoDataResponse,userId);
        }
    }


    private VideoRecordForUser findRecord(Integer videoId,Integer userId) throws InterruptedException {
        return new VideoRecordForUser(
                this.interactionService.hadCoin(userId,videoId),
                this.interactionService.findFavoriteRecord(userId,videoId),
                this.interactionService.findLikeRecord(userId,videoId));
    }

    private List<VideoDataResponse> findVideoDataOnDB(Integer userId, int offset){

        Set<Integer> valueVideoId=new HashSet<>();
        List<VideoDataResponse> videoDataResponses;

        int offsetForNew=offset%2==0 ? offset/2 : (offset+1)/2;
        int offsetForHot= offset%2==0 ? offsetForNew : offsetForNew-1;
        int limit=LIMIT/2;

        videoDataResponses=userId !=null ? this.videoDao.findVideosByUserId(userId,offsetForNew,limit,false)
                : this.videoDao.findVideos(offsetForNew,limit,false);
        if(videoDataResponses==null || videoDataResponses.isEmpty()) return null;

        videoDataResponses.addAll(
                userId!=null ? this.videoDao.findVideosByUserId(userId,offsetForHot,limit,true)
                        : this.videoDao.findVideos(offsetForHot,limit,true)
        );

        List<VideoDataResponse> valueVideoData=new ArrayList<>();

        for(VideoDataResponse videoDataResponse : videoDataResponses){
            boolean stats=valueVideoId.add(videoDataResponse.getId());
            if(stats) valueVideoData.add(videoDataResponse);
            UserResponse userResponse=videoDataResponse.getAuthor();
            if(userResponse!=null && userId!=null){
                if(userId.equals(userResponse.getId())) userResponse.setUserRelation(null);
            }
        }

        return valueVideoData;
    }

    private VideoDataResponse findVideoDataOnDB(Integer videoId,Integer userId){
        VideoDataResponse videoResponseData= this.videoDao.findVideoData(videoId, userId);
        if(videoResponseData==null) return null;

        if(userId!=null){
            UserResponse userResponse=videoResponseData.getAuthor();
            if(userResponse!=null &&  userId.equals(userResponse.getId())) userResponse.setUserRelation(null);
        }
        this.setVideoDataInCache(videoResponseData,userId);
        return videoResponseData;
    }


    private VideoDataResponse fineVideoResponseDataOnCache(String videoField,String userField) throws InterruptedException {
        Integer userId = userField != null ? Integer.parseInt(userField) : null;
        Integer videoId=Integer.parseInt(videoField);
        VideoDataResponse dataResponse = this.getVideoDataInCache(videoId);

        if(dataResponse!=null){
            if(dataResponse.getVideoStats()==null){
                VideoStats videoStats= this.videoStatsService.getVideoStats(videoId);
                dataResponse.setVideoStats(videoStats);
            }
            UserResponse userResponse =dataResponse.getAuthor();
            if(userResponse==null){
                dataResponse.setAuthor(this.userDataService.getUserResponseData(dataResponse.getAuthorId()));
            }
            if(userId!=null){
                if(userResponse!=null)
                    userResponse.setUserRelation(
                            userId.equals(userResponse.getId()) ? null :
                            new UserRelation(this.interactionService.findRelation(userId,userResponse.getId()))
                    );
                dataResponse.setVideoRecordForUser(this.findRecord(videoId,userId));
            }

            return dataResponse;
        }

        return this.findVideoDataOnDB(videoId, userId);
    }

    private VideoDataResponse findVideoDataOnCache(Integer videoId,Integer userId) throws InterruptedException {
        VideoDataResponse data= this.getVideoDataInCache(videoId);
        if(data!=null){
            if(data.getVideoStats()==null){
                VideoStats videoStats= this.videoStatsService.getVideoStats(videoId);
                data.setVideoStats(videoStats);
            }
            UserResponse userResponse=data.getAuthor();
            if(userResponse==null){
                userResponse=this.userDataService.getUserDataWithStats(data.getAuthorId());
                data.setAuthor(userResponse);
            }

            if(userId!=null){
                if(userResponse!=null ) {
                    if(userResponse.getUserRelation()==null)
                        userResponse.setUserRelation(userId.equals(userResponse.getId()) ? null :
                                new UserRelation(this.interactionService.findRelation(userId, userResponse.getId())));
                    if(userResponse.getUserStats()==null) userResponse.setUserStats(this.userStatsService.getUserStats(userResponse.getId()));
                }

                if(data.getVideoRecordForUser()==null) data.setVideoRecordForUser(this.findRecord(videoId,userId));
            }

            return data;
        }else {
            return this.findVideoDataOnDB(videoId,userId);
        }
    }

    private List<VideoDataResponse> findVideoDataOnCache(Integer userId,int offset) throws InterruptedException {
        int offsetForNew=offset%2==0 ? offset/2 : (offset+1)/2;
        int offsetForHot= offset%2==0 ? offsetForNew : offsetForNew-1;
        int limit=LIMIT/2;

        Set<Object> videoIds;
        videoIds=this.redis.zRange(RedisKeyConstant.VIDEO_RANKING_KEY,offsetForNew,offsetForNew+limit-1);
        if(videoIds==null || videoIds.isEmpty()) return null;
        Set<Object> ids=this.redis.zRevRange(RedisKeyConstant.VIDEO_RANKING_KEY,offsetForHot,offsetForHot+limit-1);
        videoIds.addAll(ids);

        List<VideoDataResponse> videoDataResponses=new ArrayList<>();
        for(Object videoId : videoIds){
            videoDataResponses.add(this.fineVideoResponseDataOnCache(videoId.toString(),userId!=null ? userId.toString() : null));
        }

        return videoDataResponses;
    }

    //这里可以使用异步
    private void setVideoClipDataInCache(Integer videoId,List<VideoClipsResponse> clipsResponses,boolean quality){
        if(videoId==null || clipsResponses==null) return;
        Map<String, Object> videoClips = new HashMap<>();
        for (VideoClipsResponse clipsResponse : clipsResponses) {
            try{
                ObjectMapper mapper=new ObjectMapper();
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
        ObjectMapper mapper=new ObjectMapper();
        List<VideoClipsResponse> videoClipsResponses=new ArrayList<>();
        for(Object videoClip : videoClips){
            if(videoClip!=null){
                VideoClipsResponse videoClipsResponse = this.redis.deserialize((String) videoClip,mapper.constructType(VideoClipsResponse.class));
                if(videoClipsResponse!=null) videoClipsResponses.add(videoClipsResponse);
            }
        }

        return videoClipsResponses;
    }

    @Override
    public List<VideoDataResponse> videoRecommendationsByRandom(Integer userId, int offset) throws InterruptedException {
        List<VideoDataResponse> videoDataResponses ;

        if(videoFractionStatsService.sizeForFraction()<=offset){
            Boolean isLock=redis.setIfAbsent(this.VIDEO_LOCK_BY_OFFSET(offset),
                    RedisKeyConstant.LOCK_VALUE,RedisKeyConstant.EXPIRED);
            if(!isLock){
                Thread.sleep(RedisKeyConstant.EXPIRED);
                videoDataResponses=this.findVideoDataOnCache(userId,offset);
                if(videoDataResponses!=null) return videoDataResponses;
            }
            videoDataResponses= this.findVideoDataOnDB(null,offset);
            this.updateVideoCache(videoDataResponses,null);

            return videoDataResponses;
        }else
            return this.findVideoDataOnCache(null,offset);
    }

    public List<VideoDataResponse> videoRecommendationsByRandom(int offset) throws InterruptedException{
        return videoRecommendationsByRandom(null,offset);
    }

    @Override
    public List<VideoDataResponse> videoRecommendationsByCategory(Integer userId, VideoCategory category, int offset) throws InterruptedException {
        int offsetForNew= offset % 2 ==0 ? offset/2 : (offset+1)/2;
        int offsetForHot= offset % 2 ==0 ? offsetForNew : offsetForNew-1;
        int limit =LIMIT/2;

        List<Integer> videos;
        videos=this.videoDao.findVideosIdsByCategory(category.getName(),offsetForNew,limit,false);
        if(videos!=null && !videos.isEmpty()) videos.addAll(this.videoDao.findVideosIdsByCategory(category.getName(),offsetForHot,limit,true));

        if(videos==null || videos.isEmpty()) return null;
        Set<Integer> ids = new HashSet<>(videos);

        List<VideoDataResponse> dataResponses =new ArrayList<>();
        for(Integer id : ids){
            VideoDataResponse videoDataResponse =this.findVideoDataOnCache(id,userId);
            if(videoDataResponse!=null) dataResponses.add(videoDataResponse);
        }
        return dataResponses;
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
            Set<Object> videoIds= redis.zRange(RedisKeyConstant.VIDEO_RANKING_KEY,0,MAX_HOT_VIDEO_SIZE);
            if(videoIds==null || videoIds.isEmpty()) return null;

            List<VideoDataResponse> videoDataResponses=new ArrayList<>();
            for(Object videoId : videoIds){
                VideoDataResponse dataResponse= this.fineVideoResponseDataOnCache(videoId.toString(),null);
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
