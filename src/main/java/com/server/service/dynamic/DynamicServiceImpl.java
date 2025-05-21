package com.server.service.dynamic;

import com.server.dao.video.VideoDao;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.constant.RedisKeyConstant;
import com.server.push.entity.Notification;
import com.server.service.videoservice.VideoService;
import com.server.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DynamicServiceImpl implements DynamicService{

    @Autowired private VideoDao videoDao;
    @Autowired private VideoService videoService;

    private final int MAX_DYNAMIC_LIST_SIZE=100;
    private final int VIDEO_LIMIT_SIZE=10;

    private static final ConcurrentHashMap<Integer,Cache> VIDEO_IDS_CACHE=new ConcurrentHashMap<>();

    public static class Cache{
        private final Lock lock =new ReentrantLock();
        private final long expire =System.currentTimeMillis()+3L*24*60*60*1000;
        private final List<Integer> videoIds = new ArrayList<>();

        public Lock getLock(){
            return lock;
        }

        public List<Integer> getVideoIds() {
            return videoIds;
        }

        public long getExpire() {
            return expire;
        }
    }

    @Override
    public void setVideoIdsInCache(Integer authorId,Integer videoIds){
        Cache cache = VIDEO_IDS_CACHE.computeIfAbsent(authorId,k->new Cache());
        Lock lock= cache.getLock();
        lock.lock();
        try{
            List<Integer> ids = cache.getVideoIds();
            if(ids.size()==MAX_DYNAMIC_LIST_SIZE){
                ids.remove(0);
            }

            ids.add(videoIds);

        }finally {
            lock.unlock();
        }
    }

    private List<Integer> findVideoIdsByAuthorId(Integer authorId,int offset){
        Cache cache = VIDEO_IDS_CACHE.computeIfAbsent(authorId,k->new Cache());
        List<Integer> ids= cache.getVideoIds();
        if(ids.isEmpty()) return ids;
        if(ids.size()<=offset) return null;
        return ids.subList(offset,Math.min(ids.size(),offset+VIDEO_LIMIT_SIZE));
    }

    private List<VideoDataResponse> findVideoDataOnCache(Integer authorId, @Nullable Timestamp lastCreated,int offset) throws InterruptedException {
        List<Integer> videoIds = findVideoIdsByAuthorId(authorId,offset);
        if(videoIds==null) return null;

        if(videoIds.isEmpty()){
            Cache cache =VIDEO_IDS_CACHE.get(authorId);
            Lock lock =cache.getLock();

            lock.lock();

            try{
                videoIds = findVideoIdsByAuthorId(authorId,offset);
                if(videoIds!=null && !videoIds.isEmpty()){
                    List<VideoDataResponse> dataResponses = new ArrayList<>();
                    for(Integer id : videoIds){
                        dataResponses.add(videoService.getVideoResponseData(id,null));
                    }

                    return dataResponses;
                }


                List<VideoDataResponse> dataResponses= this.videoDao.findVideoForDynamicByAuthor(authorId,lastCreated,MAX_DYNAMIC_LIST_SIZE);
                List<Integer> ids= cache.getVideoIds();
                if(dataResponses==null || dataResponses.isEmpty()){
                    ids.add(0);
                    return null;
                }else {
                    for(VideoDataResponse dataResponse : dataResponses.subList(0,Math.min(dataResponses.size(),MAX_DYNAMIC_LIST_SIZE))){
                        ids.add(dataResponse.getId());
                    }

                    return dataResponses;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }else {
            List<VideoDataResponse> dataResponses = new ArrayList<>();
            for(Integer id : videoIds){
                dataResponses.add(videoService.getVideoResponseData(id,null));
            }

            return dataResponses;
        }
    }

    /**
     * 查询 某user的创作
     * @param lastCreated 上一video的 时间戳
     * @param offset 在缓存的偏移量
     * @return List<VideoDataResponse> | [] | null
     * @throws InterruptedException
     */
    @Override
    public List<VideoDataResponse> findVideoDataByAuthorId(Integer authorId, @Nullable Timestamp lastCreated,int offset) throws InterruptedException {
        return  offset<MAX_DYNAMIC_LIST_SIZE
                ? findVideoDataOnCache(authorId,lastCreated,offset)
                : videoDao.findVideoForDynamicByAuthor(authorId,lastCreated,VIDEO_LIMIT_SIZE);
    }


    @Override
    public List<VideoDataResponse> findVideoByUserId(Integer userId,Timestamp lastCreated) {
        return videoDao.findVideoForDynamic(userId,lastCreated,VIDEO_LIMIT_SIZE);
    }

    /**
     * 凌晨1点半更新
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void clarnCache(){

        Set<Integer> ids = new HashSet<>();
        for (Map.Entry<Integer,Cache> map : VIDEO_IDS_CACHE.entrySet()){
            Cache cache = map.getValue();
            if(cache.getExpire()<=System.currentTimeMillis()){
                ids.add(map.getKey());
            }
        }

        for(Integer id :ids){
            VIDEO_IDS_CACHE.remove(id);
        }
    }
}
