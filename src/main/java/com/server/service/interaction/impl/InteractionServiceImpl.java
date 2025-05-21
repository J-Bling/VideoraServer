package com.server.service.interaction.impl;

import com.server.dao.stats.UserStatsDao;
import com.server.dao.user.UserRelationDao;
import com.server.dao.videointeraction.CoinRecordDao;
import com.server.dao.videointeraction.FavoritesRecordDao;
import com.server.dao.videointeraction.LikeRecordDao;
import com.server.dto.response.video.record.VideoRecordForUser;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.user.UserRelation;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.push.service.NotificationService;
import com.server.service.interaction.InteractionService;
import com.server.service.stats.UserStatsService;
import com.server.service.stats.VideoStatsService;
import com.server.util.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Service
public class InteractionServiceImpl implements InteractionService {
    @Autowired private RedisUtil redis;
    @Autowired private UserStatsDao userStatsDao;
    @Autowired private LikeRecordDao likeRecordDao;
    @Autowired private CoinRecordDao coinRecordDao;
    @Autowired private UserRelationDao userRelationDao;
    @Autowired private UserStatsService userStatsService;
    @Autowired private VideoStatsService videoStatsService;
    @Autowired private FavoritesRecordDao favoritesRecordDao;


    private static final String IS=RedisKeyConstant.IS;
    private static final String NO=RedisKeyConstant.NO;
    private static final long EXPIRE= RedisKeyConstant.EXPIRED;
    private static final String NULL=RedisKeyConstant.NULL_RECORD;
    private static final String LOCK_VALUE=RedisKeyConstant.LOCK_VALUE;
    private final Logger logger = LoggerFactory.getLogger(InteractionServiceImpl.class);

    private static final String UPDATE_COIN_KEY=RedisKeyConstant.UPDATE_COIN_KEY;
    private static final String UPDATE_FAVORITE_KEY=RedisKeyConstant.UPDATE_FAVORITE_KEY;
    private static final String UPDATE_LIKE_HASH_KEY=RedisKeyConstant.UPDATE_LIKE_HASH_KEY;
    private static final String UPDATE_USER_RELATION_KEY=RedisKeyConstant.UPDATE_USER_RELATION_KEY;

    private static String COIN_LOCK(Integer userId){
        return RedisKeyConstant.COIN_LOCK+userId;
    }
    private static String HASH_FIELD(Integer userId,Integer targetId){
        return userId+":"+targetId;
    }
    private static String USER_RELATION_HASH_KEY(Integer userId,Integer targetId){
        return RedisKeyConstant.USER_RELATION_HASH_KEY+HASH_FIELD(userId,targetId);
    };
    private static String LIKE_HASH_KEY(Integer userId,Integer videoId){
        return RedisKeyConstant.LIKE_HASH_KEY+HASH_FIELD(userId,videoId);
    };
    private static String FAVORITE_RECORD_HASH_KEY(Integer userId,Integer videoId){
        return RedisKeyConstant.FAVORITE_RECORD_HASH_KEY+HASH_FIELD(userId,videoId);
    }
    private static String COIN_RECORD_HASH_KEY(Integer userId,Integer videoId){
        return RedisKeyConstant.COIN_RECORD_HASH_KEY+HASH_FIELD(userId,videoId);
    }
    private static  String USER_RELATION_LOCK(Integer userId,Integer targetId){
        return RedisKeyConstant.USER_RELATION_LOCK+"u:"+userId+"t:"+targetId;
    }
    private static String LIKE_RECORD_LOCK(Integer userId,Integer videoId){
        return RedisKeyConstant.VIDEO_LIKE_LOCK+"u:"+userId+"v:"+videoId;
    }
    private static String FAVORITE_RECORD_LOCK(Integer userId,Integer videoId){
        return RedisKeyConstant.FAVORITE_RECORD_LOCK+"u:"+userId+"v:"+videoId;
    }
    private static String COIN_RECORD_LOCK(Integer userId,Integer videoId){
        return RedisKeyConstant.COIN_RECORD_LOCK+"u:"+userId+"v:"+videoId;
    }


    @Override
    public Boolean findRelation(Integer userId,Integer targetId) throws InterruptedException {
        String action = this.redis.get(USER_RELATION_HASH_KEY(userId,targetId));

        if(action==null){
            Boolean type = this.userRelationDao.findRelationType(userId,targetId);

            if (type==null) redis.set(USER_RELATION_HASH_KEY(userId,targetId),NULL,RedisKeyConstant.CLEAN_CACHE_SPACED);
            else redis.set(USER_RELATION_HASH_KEY(userId,targetId),type ? IS : NO,RedisKeyConstant.CLEAN_CACHE_SPACED);
            return type;
        }

        if(NULL.equals(action)) {
            return null;
        }

        return IS.equals(action);
    }

    private void pushUserRelationCache(Integer userId,Integer targetId,String type){
        redis.set(USER_RELATION_HASH_KEY(userId,targetId),type,RedisKeyConstant.CLEAN_CACHE_SPACED);
        redis.hSet(UPDATE_USER_RELATION_KEY,HASH_FIELD(userId,targetId),type);
    }

    @Override
    public Boolean handleUserRelation(UserRelation userRelation) throws InterruptedException {
        if(!userRelation.isVail() || userRelation.getUser_id().equals(userRelation.getTarget_id()))
            throw new ApiException(ErrorCode.BAD_REQUEST);

        Integer userId =userRelation.getUser_id();
        Integer targetId= userRelation.getTarget_id();

        Boolean isLock = redis.setIfAbsent(USER_RELATION_LOCK(userId,targetId),RedisKeyConstant.COIN_LOCK,RedisKeyConstant.EXPIRED);
        if(isLock==null || !isLock) throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);

        Boolean relationType=this.findRelation(userId,targetId);

        if(userRelation.getRelation_type()==null){
            if(relationType==null) throw new ApiException(ErrorCode.BAD_REQUEST);
            pushUserRelationCache(userId,targetId,NULL);
            if(relationType){
                userStatsService.CountFollowing(userId,-1);
                userStatsService.CountFollower(targetId,-1);
            }

        } else if (userRelation.getRelation_type()) {
            if(relationType!=null && relationType) throw new ApiException(ErrorCode.BAD_REQUEST);
            pushUserRelationCache(userId,targetId,IS);
            userStatsService.CountFollowing(userId,1);
            userStatsService.CountFollower(targetId,1);

        }else {
            if(relationType!=null){
                if(!relationType) throw new ApiException(ErrorCode.BAD_REQUEST);
                else {
                    userStatsService.CountFollowing(userId,-1);
                    userStatsService.CountFollower(targetId,-1);
                }
            }

            pushUserRelationCache(userId,targetId,NO);
        }

        return userRelation.getRelation_type();
    }



    @Override
    public Boolean findLikeRecord(Integer userId,Integer videoId) throws InterruptedException {
        if(userId==null || videoId==null) throw new ApiException(ErrorCode.BAD_REQUEST);

        String isLike=redis.get(LIKE_HASH_KEY(userId,videoId));
        if(NULL.equals(isLike)) return null;

        if(isLike==null){
            Boolean isLock = redis.setIfAbsent(LIKE_RECORD_LOCK(userId,videoId),LOCK_VALUE,EXPIRE);

            if(isLock!=null && !isLock){
                Thread.sleep(EXPIRE);
                isLike=redis.get(LIKE_HASH_KEY(userId,videoId));

                if(NULL.equals(isLike) || isLike==null) return null;

                return true;
            }

            Boolean type= this.likeRecordDao.findIsLike(userId,videoId);

            if(type==null) redis.set(LIKE_HASH_KEY(userId,videoId),NULL,RedisKeyConstant.CLEAN_CACHE_SPACED);
            else redis.set(LIKE_HASH_KEY(userId,videoId),IS,RedisKeyConstant.CLEAN_CACHE_SPACED);

            return type;
        }

        return true;
    }

    @Override
    public Boolean handleLikeForVideo(Integer userId, Integer videoId, Integer authorId,Integer like) throws InterruptedException {
        Boolean isLock = redis.setIfAbsent(LIKE_RECORD_LOCK(userId,videoId),RedisKeyConstant.COIN_LOCK,RedisKeyConstant.EXPIRED);
        if(isLock==null || !isLock) throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);

        Boolean isLike =this.findLikeRecord(userId,videoId);

        if(like==0){ //取消
            if(isLike==null) throw new ApiException(ErrorCode.BAD_REQUEST);
            redis.set(LIKE_HASH_KEY(userId,videoId),NULL,RedisKeyConstant.CLEAN_CACHE_SPACED);
            redis.hSet(UPDATE_LIKE_HASH_KEY,HASH_FIELD(userId,videoId),NULL);
            videoStatsService.CountLike(videoId,-1);
            return false;

        }else if(like==1){//点赞
            if(isLike!=null && isLike) throw new ApiException(ErrorCode.BAD_REQUEST);
            redis.set(LIKE_HASH_KEY(userId,videoId),IS,RedisKeyConstant.CLEAN_CACHE_SPACED);
            redis.hSet(UPDATE_LIKE_HASH_KEY,HASH_FIELD(userId,videoId),IS);
            videoStatsService.CountLike(videoId,1);
            return true;

        }else throw new ApiException(ErrorCode.BAD_REQUEST);
    }


    @Override
    public Boolean findFavoriteRecord(Integer userId,Integer videoId) throws InterruptedException{
        if(userId==null || videoId==null) throw new ApiException(ErrorCode.BAD_REQUEST);

        String isFav = redis.get(FAVORITE_RECORD_HASH_KEY(userId,videoId));

        if(isFav==null){
            Boolean isLock=redis.setIfAbsent(FAVORITE_RECORD_LOCK(userId,videoId),LOCK_VALUE,EXPIRE);

            if(isLock!=null && !isLock){
                Thread.sleep(EXPIRE);
                isFav = redis.get(FAVORITE_RECORD_HASH_KEY(userId,videoId));

                if(NULL.equals(isFav) || isFav==null) return null;

                return true;
            }

            Boolean type = this.favoritesRecordDao.findFavType(userId,videoId);
            if(type==null) redis.set(FAVORITE_RECORD_HASH_KEY(userId,videoId),NULL,RedisKeyConstant.CLEAN_CACHE_SPACED);
            else redis.set(FAVORITE_RECORD_HASH_KEY(userId,videoId),IS,RedisKeyConstant.CLEAN_CACHE_SPACED);

            return type;
        }

        return NULL.equals(isFav) ? null : true;
    }

    @Override
    public Boolean handleFavFoeVideo(Integer userId, Integer videoId, Integer fav) throws InterruptedException {
        Boolean isLock = redis.setIfAbsent(FAVORITE_RECORD_LOCK(userId,videoId)+":handle",RedisKeyConstant.COIN_LOCK,RedisKeyConstant.EXPIRED);
        if(isLock==null || !isLock) throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);

        Boolean isFav =this.findFavoriteRecord(userId,videoId);
        String field=HASH_FIELD(userId,videoId);

        if(fav==1){
            if(isFav!=null) throw new ApiException(ErrorCode.BAD_REQUEST);

            redis.set(FAVORITE_RECORD_HASH_KEY(userId,videoId),IS,RedisKeyConstant.CLEAN_CACHE_SPACED);
            redis.hSet(UPDATE_FAVORITE_KEY,field,IS);
            userStatsService.CountFavorite(userId,1);
            videoStatsService.CountFavorite(videoId,1);

            return true;

        }else if(fav==0){
            if(isFav==null) throw new ApiException(ErrorCode.BAD_REQUEST);

            redis.set(FAVORITE_RECORD_HASH_KEY(userId,videoId),NULL,RedisKeyConstant.CLEAN_CACHE_SPACED);
            redis.hSet(UPDATE_FAVORITE_KEY,field,NULL);
            userStatsService.CountFavorite(userId,-1);
            videoStatsService.CountFavorite(videoId,-1);

            return false;

        }else throw new ApiException(ErrorCode.BAD_REQUEST);
    }


    @Override
    public Boolean hadCoin(Integer userId,Integer videoId) throws InterruptedException {
        if(userId==null || videoId == null) throw new ApiException(ErrorCode.BAD_REQUEST);

        String isCoin =redis.get(COIN_RECORD_HASH_KEY(userId,videoId));

        if(isCoin==null){
            Boolean isLock=this.redis.setIfAbsent(COIN_RECORD_LOCK(userId,videoId),LOCK_VALUE,EXPIRE);

            if(isLock){
                Integer status=this.coinRecordDao.existsCoined(videoId,userId);

                isCoin=status==null || status==0 ? NULL : IS;
                redis.set(COIN_RECORD_HASH_KEY(userId,videoId),isCoin,RedisKeyConstant.CLEAN_CACHE_SPACED);

            }else {
                Thread.sleep(EXPIRE);
                isCoin =redis.get(COIN_RECORD_HASH_KEY(userId,videoId));
            }
        }

        return NULL.equals(isCoin) ? null : true;
    }


    private Integer setCoinOnCache(Integer userId){
        Integer coins = userStatsDao.findCoinByUserId(userId);
        if(coins==null) throw new ApiException(ErrorCode.BAD_REQUEST);
        Map<String,Object> coinColumn=new HashMap<>();
        coinColumn.put(RedisKeyConstant.USER_COIN_COUNT, ""+coins);
        redis.hmSet(RedisKeyConstant.USER_STATS+userId,coinColumn,RedisKeyConstant.CLEAN_CACHE_SPACED);
        return coins;
    }

    @Override
    public void handleCoinForVideo(Integer userId, Integer videoId) throws InterruptedException {
        Boolean isLock = redis.setIfAbsent(COIN_LOCK(userId),RedisKeyConstant.COIN_LOCK,RedisKeyConstant.EXPIRED);
        if(isLock==null || !isLock) throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
        try {
            Boolean hasCoined = this.hadCoin(userId, videoId);
            if (hasCoined != null && hasCoined) throw new ApiException(ErrorCode.OPERATION_REPEATED);
            String coinsStr = redis.hGet(RedisKeyConstant.USER_STATS + userId, RedisKeyConstant.USER_COIN_COUNT).toString();
            Integer coins = coinsStr != null ? Integer.parseInt(coinsStr) : setCoinOnCache(userId);

            if (coins == null || coins < 1) throw new ApiException(ErrorCode.PAYMENT_REQUIRED);

            userStatsService.CountCoin(userId, -1);
            videoStatsService.CountCoin(videoId, 1);
            redis.hSet(UPDATE_COIN_KEY, HASH_FIELD(userId, videoId), IS);
            redis.set(COIN_RECORD_HASH_KEY(userId, videoId), IS, RedisKeyConstant.CLEAN_CACHE_SPACED);

        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Handle coin for video failed. userId: {}, videoId: {}", userId, videoId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public void setRecordOnCache(VideoRecordForUser record,Integer userId,Integer videoId) {
        if(record==null || userId==null || videoId==null) return;
        redis.set(
                COIN_RECORD_HASH_KEY(userId,videoId),
                record.getHadCoin()==null || !record.getHadCoin() ? NULL : IS,
                RedisKeyConstant.CLEAN_CACHE_SPACED
        );
        redis.set(
                LIKE_HASH_KEY(userId,videoId),
                record.getHadLike()==null || !record.getHadLike() ? NULL : IS,
                RedisKeyConstant.CLEAN_CACHE_SPACED
        );
        redis.set(
                FAVORITE_RECORD_HASH_KEY(userId,videoId),
                record.getHadFavorites()==null || !record.getHadFavorites() ? NULL : IS,
                RedisKeyConstant.CLEAN_CACHE_SPACED
        );
    }

    @Override
    public void setRelationOnCache(UserRelation relation) {
        if(relation ==null || relation.getUser_id()==null || relation.getTarget_id()==null)
            return;
        this.redis.set(
                USER_RELATION_HASH_KEY(relation.getUser_id(),relation.getTarget_id()),
                relation.getRelation_type()==null ? NULL: relation.getRelation_type() ? IS:NO,
                RedisKeyConstant.CLEAN_CACHE_SPACED
        );
    }
}
