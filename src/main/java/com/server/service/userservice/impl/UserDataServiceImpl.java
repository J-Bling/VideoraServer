package com.server.service.userservice.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.dao.user.UserDao;
import com.server.dto.request.UserRequestBase;
import com.server.dto.response.user.UserResponse;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.user.User;
import com.server.entity.user.UserRelation;
import com.server.service.interaction.InteractionService;
import com.server.service.stats.UserStatsService;
import com.server.service.userservice.UserDataService;
import com.server.util.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserDataServiceImpl implements UserDataService {

    @Autowired private UserDao userDao;
    @Autowired private RedisUtil redis;
    @Autowired private UserStatsService userStatsService;
    @Autowired private InteractionService interactionService;


    private static String USER_DATA_KEY(Integer userId){
        return RedisKeyConstant.USER_DATA_KEY+userId;
    }
    private static String USER_DATA_LOCK(Integer userId){
        return RedisKeyConstant.USER_LOCK+userId;
    }
    private final Logger logger= LoggerFactory.getLogger(UserDataServiceImpl.class);

    private void setUserDataInCache(UserResponse userResponse){
        if(userResponse==null) return;
        ObjectMapper mapper=new ObjectMapper();
        try{
            String data=mapper.writeValueAsString(userResponse);
            redis.set(USER_DATA_KEY(userResponse.getId()),data,RedisKeyConstant.CLEAN_CACHE_SPACED);
        }catch (Exception e){
            logger.error("",e);
        }
    }

    private UserResponse getUserDataInCache(Integer userId){
        String user=redis.get(USER_DATA_KEY(userId));
        if(user==null) return null;
        ObjectMapper mapper=new ObjectMapper();
        return redis.deserialize(user,mapper.constructType(UserResponse.class));
    }


    /**
     *  just find : id,nickname,gender,avatar_url,description
     * @param userId int
     * @return UserResponse ->{id,nickname,gender,avatar_url,description }
     */
    private UserResponse findUserDataOnDb(Integer userId){
        UserResponse userResponse=this.userDao.findUserDataById(userId);
        updateUserDataOnCache(userResponse);
        return userResponse;
    }


    /**
     * Function : setUserStatsOnCache and setRelationOnCache can filter unValue
     * @param userResponse
     */
    private void updateUserDataOnCache(UserResponse userResponse){
        if(userResponse==null) return;
        this.setUserDataInCache(userResponse);
        userStatsService.setUserStatsOnCache(userResponse.getUserStats());
        interactionService.setRelationOnCache(userResponse.getUserRelation());
    }

    @Override
    public UserResponse tryGetUserResponse(Integer userId){
        return this.getUserDataInCache(userId);
    }

    @Override
    public UserResponse getUserResponseData(Integer userId) throws InterruptedException {
        UserResponse userResponse= this.getUserDataInCache(userId);
        if(userResponse!=null) return userResponse;
        Boolean isLock=redis.setIfAbsent(USER_DATA_LOCK(userId),RedisKeyConstant.LOCK_VALUE,RedisKeyConstant.EXPIRED);
        if(!isLock){
            Thread.sleep(RedisKeyConstant.EXPIRED);
            userResponse= this.getUserDataInCache(userId);
            if(userResponse!=null) return userResponse;
        }

        return this.findUserDataOnDb(userId);
    }

    /**
     * @param userId int
     * @return  UserResponse :base + stats
     */
    @Override
    public UserResponse getUserDataWithStats(Integer userId) throws InterruptedException {
        if(userId==null) return null;
        UserResponse userResponse = this.getUserResponseData(userId);
        if(userResponse==null) return null;
        userResponse.setUserStats(this.userStatsService.getUserStats(userId));
        return userResponse;
    }

    /**
     *
     * @param targetId int
     * @param userId int
     * @return UserResponse :base + stats + relation
     */
    @Override
    public UserResponse getUserWithStatsAndRelation(Integer targetId, Integer userId) throws InterruptedException {
        UserResponse userResponse=this.getUserDataWithStats(targetId);
        if(userResponse==null) return null;
        userResponse.setUserRelation(new UserRelation(this.interactionService.findRelation(userId,targetId)));
        return userResponse;
    }


    /**
     *
     * @param userId int
     * @return just find User data
     */
    @Override
    public User getUserData(Integer userId) {
        return userDao.findUserById(userId);
    }

    @Override
    public User getUserData(UserRequestBase requestBase) {
        return null;
    }

    @Override
    public void setUserDataOnCache(User user){

    }

    @Override
    public void setUserResponseOnCache(UserResponse responses) {
        this.updateUserDataOnCache(responses);
    }


    @Override
    public void setUserResponseOnCache(List<UserResponse> responses) {
        for(UserResponse user : responses){
            setUserResponseOnCache(user);
        }
    }
}
