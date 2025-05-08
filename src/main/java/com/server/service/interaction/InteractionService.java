package com.server.service.interaction;

import com.server.dto.response.video.record.VideoRecordForUser;
import com.server.entity.user.UserRelation;

public interface InteractionService {
    Boolean handleUserRelation(UserRelation userRelation) throws InterruptedException;
    Boolean handleLikeForVideo(Integer userId,Integer videoId,Integer authorId,Integer like) throws InterruptedException;
    void handleCoinForVideo(Integer userId,Integer videoId) throws InterruptedException;
    Boolean handleFavFoeVideo(Integer userId,Integer videoId,Integer fav) throws InterruptedException;
    Boolean findRelation(Integer userId,Integer targetId) throws InterruptedException;
    Boolean findLikeRecord(Integer userId,Integer videoId) throws InterruptedException;
    Boolean findFavoriteRecord(Integer userId,Integer videoId) throws InterruptedException;
    Boolean hadCoin(Integer userId,Integer videoId) throws InterruptedException;
    void setRecordOnCache(VideoRecordForUser record,Integer userId,Integer videoId);
    void setRelationOnCache(UserRelation relation);
}
