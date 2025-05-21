package com.server.comment.server;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.comment.dao.CommentDao;
import com.server.comment.dto.request.CreateCommentRequest;
import com.server.comment.dto.response.CommentResponse;
import com.server.comment.entity.Comment;
import com.server.comment.entity.CommentUserActions;
import com.server.dto.response.user.UserResponse;
import com.server.entity.constant.RedisKeyConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.service.userservice.UserDataService;
import com.server.util.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommentServiceImpl implements CommentService, DisposableBean , CommandLineRunner {

    @Autowired private CommentDao commentDao;
    @Autowired private RedisUtil redis;
    @Autowired private UserDataService userDataService;


    @Value("${TEMP_DATA_DIR}") private String TEMP_DATA_DIR;
    @Value("${TEMP_DATA_VIDEO_COMMENTS_KEY_RECORD}") private String TEMP_DATA_VIDEO_COMMENTS_KEY_RECORD;
    @Value("${TEMP_DATA_REPLY_COMMENTS_KEY_RECORD}") private String TEMP_DATA_REPLY_COMMENTS_KEY_RECORD;
    @Value("${TEMP_DATA_VIDEO_COMMENTS_RANK_RECORD}") private String TEMP_DATA_VIDEO_COMMENTS_RANK_RECORD;


    private final ObjectMapper mapper = new ObjectMapper();
    private final int MAX_COMMENT_SIZE=9;
    private final int MAX_HOT_COMMENTS_SIZE=100;
    private final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);
    private final long LIKE_WEIGHT=2L;
    private final long REPLY_WEIGHT=5L;

    /**
     * 这里两个记录需要 在项目关闭时写入文件持久化 项目再次启动则需要写入缓存
     */
    private final static ConcurrentHashMap<String,Long> VIDEO_COMMENTS_KEY_RECORD=new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String,Long> REPLY_COMMENTS_KEY_RECORD=new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String,Long> VIDEO_COMMENTS_RANK_RECORD =new ConcurrentHashMap<>();


    private final String COMMENT_LIKE_COUNT_FILE="like_count";
    private final String COMMENT_REPLY_COUNT_FILE="reply_count";
    private String COMMENT_FOR_VIDEO_LIST_KEY(Integer videoId){
        return RedisKeyConstant.COMMENT_FOR_VIDEO_LIST_KEY+videoId;
    }
    private String COMMENT_FOR_ROOT_LIST_KEY(String rootId){
        return RedisKeyConstant.COMMENT_FOR_ROOT_LIST_KEY+rootId;
    }
    private String COMMENT_SCORE_RANK(Integer videoId){
        return RedisKeyConstant.COMMENT_SCORE_RANK+videoId;
    }
    private String COMMENT_VIDEO_OFFSET_LOCK(Integer videoId,Integer offset){
        return RedisKeyConstant.COMMENT_VIDEO_OFFSET_LOCK+videoId + RedisKeyConstant.INTERVAL_NUMBER + offset;
    }
    private String COMMENT_REPLY_OFFSET_LOCK(String rootId,Integer offset){
        return RedisKeyConstant.COMMENT_REPLY_OFFSET_LOCK+rootId+RedisKeyConstant.INTERVAL_NUMBER+offset;
    }
    private String COMMENT_ACTION_KEY(String commentId,Integer userId){
        return RedisKeyConstant.COMMENT_ACTION_KEY+userId+RedisKeyConstant.INTERVAL_NUMBER+commentId;
    }
    private String COMMENT_STATS_HASH_FILE_KEY(String commentId,String file){
        return commentId+RedisKeyConstant.INTERVAL_NUMBER+file;
    }
    private String COMMENT_COUNT_UPDATE_HASH_FILE(String commentId,String file){
        return commentId + RedisKeyConstant.INTERVAL_NUMBER +file;
    }
    private String COMMENT_STRING_KEY(String commentId){
        return RedisKeyConstant.COMMENT_STRING_KEY + commentId;
    }
    private String COMMENT_ACTION_CLOC(String commentId,Integer userId){
        return RedisKeyConstant.COMMENT_ACTION_CLOC+ commentId + RedisKeyConstant.INTERVAL_NUMBER + userId;
    }


    private void setCommentStats(Comment comment){
        if(comment==null) return;
        String file1 = COMMENT_STATS_HASH_FILE_KEY(comment.getComment_id(),COMMENT_LIKE_COUNT_FILE);
        String file2 = COMMENT_STATS_HASH_FILE_KEY(comment.getComment_id(),COMMENT_REPLY_COUNT_FILE);

        redis.hSet(RedisKeyConstant.COMMENT_STATS_HASH_KEY,file1,comment.getLike_count().toString());
        redis.hSet(RedisKeyConstant.COMMENT_STATS_HASH_KEY,file2,comment.getReply_count().toString());
    }

    private Map<String,Integer> getCommentStats(String commentId){
        String file1 = COMMENT_STATS_HASH_FILE_KEY(commentId,COMMENT_LIKE_COUNT_FILE);
        String file2 = COMMENT_STATS_HASH_FILE_KEY(commentId,COMMENT_REPLY_COUNT_FILE);

        Map<String,Integer> map = new HashMap<>();
        Object obj1= redis.hGet(RedisKeyConstant.COMMENT_STATS_HASH_KEY,file1);

        if(obj1==null){
            Comment comment=getCommentOnCache(commentId);
            if(comment==null) return null;

            comment.setComment_id(commentId);
            setCommentStats(comment);

            map.put(COMMENT_LIKE_COUNT_FILE,comment.getLike_count());
            map.put(COMMENT_REPLY_COUNT_FILE,comment.getReply_count());
            return map;
        }

        Object obj2 = redis.hGet(RedisKeyConstant.COMMENT_STATS_HASH_KEY,file2);

        map.put(COMMENT_LIKE_COUNT_FILE,Integer.parseInt(obj1.toString()));
        map.put(COMMENT_REPLY_COUNT_FILE,Integer.parseInt(obj2.toString()));
        return map;
    }

    private void increaseCount(String commentId,String fileName,long count){
        String file = COMMENT_STATS_HASH_FILE_KEY(commentId,fileName);
        Boolean exists = redis.hExists(RedisKeyConstant.COMMENT_STATS_HASH_KEY,file);
        if(exists==null || !exists) return;

        redis.hInCrBy(RedisKeyConstant.COMMENT_STATS_HASH_KEY,file,count);
    }

    private void increaseLikeCount(String commentId,long count,Integer videoId){
        increaseCount(commentId,COMMENT_LIKE_COUNT_FILE,count);
        redis.hInCrBy(RedisKeyConstant.COMMENT_COUNT_UPDATE_HASH_KEY,COMMENT_COUNT_UPDATE_HASH_FILE(commentId,COMMENT_LIKE_COUNT_FILE),count);
        String rankKey = COMMENT_SCORE_RANK(videoId);
        Double score = redis.getRedisTemplate().opsForZSet().score(rankKey,commentId);
        if(score!=null){
            redis.zIncrBy(rankKey,commentId,count*LIKE_WEIGHT);
        }
    }

    private void increaseReplyCount(String commentId,long count,Integer videoId){
        increaseCount(commentId,COMMENT_REPLY_COUNT_FILE,count);
        redis.hInCrBy(RedisKeyConstant.COMMENT_COUNT_UPDATE_HASH_KEY,COMMENT_COUNT_UPDATE_HASH_FILE(commentId,COMMENT_REPLY_COUNT_FILE),count);
        String rankKey = COMMENT_SCORE_RANK(videoId);
        Double score = redis.getRedisTemplate().opsForZSet().score(rankKey,commentId);
        if(score!=null){
            redis.zIncrBy(rankKey,commentId,count*REPLY_WEIGHT);
        }
    }


    @Override
    public String createComment(CreateCommentRequest request, int authorId) {
        if(!request.vail()){
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        Comment comment = request.toComment(authorId);
        try{
            String commentStr = mapper.writeValueAsString(comment);
            redis.rPush(RedisKeyConstant.COMMENT_INSERT_LIST_KEY,commentStr);
            redis.lPush(
                    request.getRoot_id()==null
                        ? COMMENT_FOR_VIDEO_LIST_KEY(request.getVideo_id())
                        : COMMENT_FOR_ROOT_LIST_KEY(request.getRoot_id()),
                    commentStr
            );
            redis.zAdd(COMMENT_SCORE_RANK(comment.getVideo_id()),0,comment.getComment_id());
            if(request.getRoot_id()!=null){
                increaseReplyCount(request.getParent_id(),1,request.getVideo_id());
                if(!request.getRoot_id().equals(request.getParent_id())){
                    increaseReplyCount(request.getRoot_id(),1,request.getVideo_id());
                }
                REPLY_COMMENTS_KEY_RECORD.putIfAbsent(request.getRoot_id(),System.currentTimeMillis());
            }else {
                VIDEO_COMMENTS_KEY_RECORD.putIfAbsent(request.getVideo_id().toString(),System.currentTimeMillis());
            }

            redis.hSet(RedisKeyConstant.COMMENT_STATS_HASH_KEY,COMMENT_STATS_HASH_FILE_KEY(comment.getComment_id(),COMMENT_LIKE_COUNT_FILE),"0");
            redis.hSet(RedisKeyConstant.COMMENT_STATS_HASH_KEY,COMMENT_STATS_HASH_FILE_KEY(comment.getComment_id(),COMMENT_REPLY_COUNT_FILE),"0");

            VIDEO_COMMENTS_RANK_RECORD.putIfAbsent(request.getVideo_id().toString(),System.currentTimeMillis());
            setCommentStats(comment);

            return comment.getComment_id();
        } catch (JsonProcessingException e) {
            logger.error("序列化 comment 失败 : {}",e.getMessage());
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }



    private void setCommentOnCache(String key,List<CommentResponse> comments,@Nullable Integer userId){
        for(CommentResponse response : comments){
            try{
                String commentStr = mapper.writeValueAsString(response.getComment());
                redis.lPush(key,commentStr);

                if(userId!=null){
                    String actionStr = response.getAction()==null ? RedisKeyConstant.NULL : RedisKeyConstant.IS;
                    redis.set(COMMENT_ACTION_KEY(response.getComment().getComment_id(),userId),actionStr,RedisKeyConstant.COMMENT_ALL_LIFT);
                }

                setCommentStats(response.getComment());
                userDataService.setUserResponseOnCache(response.getAuthor());
            }catch (JacksonException jacksonException){
                logger.error("序列化comment 失败 {}",jacksonException.getMessage());
            }
        }
    }

    private CommentUserActions getActionOnCache(String commentId,@Nullable Integer userId){
        if(userId==null) return null;
        String key = COMMENT_ACTION_KEY(commentId,userId);
        String actionStr = redis.get(key);
        if(actionStr==null){
            Integer exists = commentDao.findAction(commentId,userId);
            if(exists.equals(1)){
                redis.set(key,RedisKeyConstant.IS,RedisKeyConstant.COMMENT_ALL_LIFT);
                return new CommentUserActions(commentId,userId);
            } else{
                redis.set(key,RedisKeyConstant.NULL,RedisKeyConstant.COMMENT_ALL_LIFT);
                return null;
            }

        }
        return RedisKeyConstant.IS.equals(actionStr) ? new CommentUserActions(commentId,userId) : null;
    }



    @Override
    public List<CommentResponse> getCommentsByVideoId(Integer videoId, int offset,@Nullable Integer userId) throws InterruptedException {
        String key =COMMENT_FOR_VIDEO_LIST_KEY(videoId);
        List<Object> commentStr =redis.lRange(key,offset,MAX_COMMENT_SIZE+offset);

        VIDEO_COMMENTS_KEY_RECORD.putIfAbsent(videoId.toString(),System.currentTimeMillis());

        if(commentStr==null || commentStr.isEmpty()){
            Boolean isLock=redis.setIfAbsent(COMMENT_VIDEO_OFFSET_LOCK(videoId,offset),RedisKeyConstant.LOCK_VALUE,RedisKeyConstant.EXPIRED*2);
            if(isLock!=null && !isLock){
                Thread.sleep(RedisKeyConstant.EXPIRED*2);
                return this.getCommentsByVideoId(videoId,offset,userId);
            }

            List<CommentResponse> comments =commentDao.findCommentByVideoId(videoId,userId,offset,MAX_COMMENT_SIZE+1);
            if(comments==null || comments.isEmpty()){
                if(offset==0) redis.lPush(key,RedisKeyConstant.NULL);
                return null;
            }

            setCommentOnCache(key,comments,userId);

            return comments;
        }

        if(commentStr.size()==1 && commentStr.get(0).toString().equals(RedisKeyConstant.NULL)){
            return null;
        }

        List<CommentResponse> comments = new ArrayList<>();

        for(Object object : commentStr){
            String commentS = object.toString();
            try{
                Comment comment = mapper.readValue(commentS,mapper.constructType(Comment.class));
                Map<String,Integer> map = getCommentStats(comment.getComment_id());
                if(map!=null) {
                    comment.setLike_count(map.get(COMMENT_LIKE_COUNT_FILE));
                    comment.setReply_count(map.get(COMMENT_REPLY_COUNT_FILE));
                }

                CommentUserActions actions = getActionOnCache(comment.getComment_id(),userId);
                UserResponse user = userDataService.getUserResponseData(comment.getUser_id());
                comments.add(new CommentResponse(comment,user,actions));
            }catch (JacksonException jacksonException){
                continue;
            }
        }

        return comments;
    }



    @Override
    public List<CommentResponse> getReplyByVideoId(@NotNull Integer videoId, @NotNull String rootId,@Nullable Integer userId,int offset) throws InterruptedException {
        String key =COMMENT_FOR_ROOT_LIST_KEY(rootId);
        List<Object> comments =redis.lRange(key,offset,offset+MAX_COMMENT_SIZE);

        REPLY_COMMENTS_KEY_RECORD.putIfAbsent(rootId,System.currentTimeMillis());

        if(comments==null || comments.isEmpty()){
            Boolean isLock = redis.setIfAbsent(COMMENT_REPLY_OFFSET_LOCK(rootId,offset),RedisKeyConstant.LOCK_VALUE, RedisKeyConstant.EXPIRED*2);

            if(isLock!=null && !isLock){
                Thread.sleep(RedisKeyConstant.EXPIRED*2);
                return this.getReplyByVideoId(videoId,rootId,userId,offset);
            }

            List<CommentResponse> commentResponseList = commentDao.findCommentByRootId(videoId,rootId,userId,offset,MAX_COMMENT_SIZE+1);
            if(commentResponseList==null || commentResponseList.isEmpty()){
                if(offset==0){
                    redis.lPush(key,RedisKeyConstant.NULL);
                }
                return null;
            }

            setCommentOnCache(key,commentResponseList,userId);
            return commentResponseList;
        }

        if(comments.size()==1 && RedisKeyConstant.NULL.equals(comments.get(0).toString())){
            return null;
        }

        List<CommentResponse> commentResponseList = new ArrayList<>();

        for(Object object : comments){
            String commentStr = object.toString();
            try{
                Comment comment = mapper.readValue(commentStr,mapper.constructType(Comment.class));
                Map<String,Integer> map =getCommentStats(comment.getComment_id());
                if(map!=null){
                    comment.setLike_count(map.get(COMMENT_LIKE_COUNT_FILE));
                    comment.setReply_count(map.get(COMMENT_REPLY_COUNT_FILE));
                }

                CommentUserActions actions = getActionOnCache(comment.getComment_id(),userId);
                UserResponse userResponse = userDataService.getUserResponseData(comment.getUser_id());

                commentResponseList.add(new CommentResponse(comment,userResponse,actions));
            }catch (JacksonException jacksonException){
                continue;
            }
        }

        return commentResponseList;
    }


    private void setCommentOnCache(String key,String comment){
        redis.set(key,comment,RedisKeyConstant.COMMENT_ALL_LIFT);
    }

    private void setCommentOnCache(String key , Comment comment){
        try{
            redis.set(key,mapper.writeValueAsString(comment),RedisKeyConstant.COMMENT_ALL_LIFT);
        }catch (Exception e){

        }
    }

    @Override
    public Comment getCommentOnCache(String commentId){
        String key =COMMENT_STRING_KEY(commentId);
        String str= redis.get(key);
        if(str==null) {
            Comment comment = commentDao.findCommentStats(commentId);
            if(comment==null){
                setCommentOnCache(key,RedisKeyConstant.NULL);
            }else {
                setCommentOnCache(key,comment);
            }
            return comment;
        }

        if(RedisKeyConstant.NULL.equals(str)) return null;

        try{
            return mapper.readValue(str,mapper.constructType(Comment.class));
        }catch (Exception e){
            return null;
        }
    }


    private double computedScore(Comment comment){
        return comment.getLike_count() * LIKE_WEIGHT + comment.getReply_count()*REPLY_WEIGHT;
    }

    private void setCommentOnRankCache(String key,List<CommentResponse> commentResponseList,@Nullable Integer userId){
        for(CommentResponse commentResponse : commentResponseList){
            redis.zAdd(key,computedScore(commentResponse.getComment()),commentResponse.getComment().getComment_id());

            String actionStr = commentResponse.getAction() == null ? RedisKeyConstant.NULL : RedisKeyConstant.IS;

            if(userId!=null)
                redis.set(COMMENT_ACTION_KEY(commentResponse.getComment().getComment_id(),userId),actionStr,RedisKeyConstant.COMMENT_ALL_LIFT);

            userDataService.setUserResponseOnCache(commentResponse.getAuthor());
        }
    }


    /**
     * @return List<CommentResponse> 可能是乱序的
     */
    @Override
    public List<CommentResponse> getCommentByVideoWithScore(Integer videoId, Integer userId,int offset) {
        if(offset>=MAX_HOT_COMMENTS_SIZE){
            return commentDao.findCommentByHot(videoId,userId,offset,MAX_COMMENT_SIZE+1);
        }

        VIDEO_COMMENTS_RANK_RECORD.putIfAbsent(videoId.toString(),System.currentTimeMillis());

        String key = COMMENT_SCORE_RANK(videoId);
        Set<Object> commentsObject = redis.zRange(key,offset,offset+MAX_COMMENT_SIZE);

        if(commentsObject==null || commentsObject.isEmpty()){
            List<CommentResponse> comments=commentDao.findCommentByHot(videoId,userId,offset,MAX_COMMENT_SIZE+1);

            if(comments==null || comments.isEmpty()){
                if(offset==0){
                    redis.zAdd(key,0,RedisKeyConstant.NULL);
                }
                return null;
            }

            setCommentOnRankCache(key,comments,userId);
            return comments;
        }

        if(offset==0 && commentsObject.size()==1 && RedisKeyConstant.NULL.equals(commentsObject.toArray()[0].toString())){
            return null;
        }

        List<CommentResponse> commentResponseList = new ArrayList<>();

        for(Object object : commentsObject.toArray()){
            String str = object.toString();
            try{
                Comment comment = getCommentOnCache(str);
                if(comment==null) continue;
                CommentUserActions actions = getActionOnCache(comment.getComment_id(),userId);
                UserResponse userResponse = userDataService.getUserResponseData(comment.getUser_id());

                commentResponseList.add(new CommentResponse(comment,userResponse,actions));

            }catch (Exception e){
                continue;
            }
        }

        return commentResponseList;
    }


    private void handleAction(String commentId,Integer userId,boolean isLike ,Integer videoId){
        CommentUserActions actions = getActionOnCache(commentId,userId);
        String key = COMMENT_ACTION_KEY(commentId,userId);
        if(isLike){
            if(actions!=null) throw new ApiException(ErrorCode.BAD_REQUEST);

            actions = new CommentUserActions(commentId,userId);
            try{
                String str =mapper.writeValueAsString(actions);
                redis.set(key,RedisKeyConstant.IS,RedisKeyConstant.COMMENT_ALL_LIFT);
                redis.rPush(RedisKeyConstant.COMMENT_ACTION_INSERT_LIST_KEY,str);
                redis.lRem(RedisKeyConstant.COMMENT_ACTION_DELETE_LIST_KEY,str);
                increaseLikeCount(commentId,1,videoId);
            }catch (JacksonException e){
                throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }else{
            if(actions==null) throw new ApiException(ErrorCode.BAD_REQUEST);
            try {
                String str =mapper.writeValueAsString(actions);
                redis.set(key, RedisKeyConstant.NULL, RedisKeyConstant.COMMENT_ALL_LIFT);
                redis.rPush(RedisKeyConstant.COMMENT_ACTION_DELETE_LIST_KEY, str);
                redis.lRem(RedisKeyConstant.COMMENT_ACTION_INSERT_LIST_KEY, str);
                increaseLikeCount(commentId,-1,videoId);
            }catch (JacksonException e){
                throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

    }



    @Override
    public boolean like(String commentId, Integer userId,Integer videoId) {
        try{
            /**
             *  在多个 相同commmentId userId videoId 参数 并发请求时可能会造成竞态条件
             *  使用使用分布式锁 当有锁就 return false 直接禁止掉并发 对同一资源重复行为
             */

            Boolean isLock = redis.setIfAbsent(COMMENT_ACTION_CLOC(commentId,userId),RedisKeyConstant.LOCK_VALUE,RedisKeyConstant.EXPIRED);
            if(isLock==null||!isLock) return false;
            handleAction(commentId,userId,true,videoId);
            return true;
        }catch (ApiException apiException){
            return false;
        } catch (Exception e){
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean disLike(String commentId, Integer userId,Integer videoId) {
        try {
            /**
             * 使用分布式锁
             */
            Boolean isLock = redis.setIfAbsent(COMMENT_ACTION_CLOC(commentId,userId),RedisKeyConstant.LOCK_VALUE,RedisKeyConstant.EXPIRED);
            if(isLock==null||!isLock) return false;
            handleAction(commentId,userId,false,videoId);
            return true;
        }catch (Exception e){
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteComment(String commentId, Integer userId,Integer videoId) {
        Comment comment = commentDao.findComment(commentId,userId);
        if(comment==null) throw new ApiException(ErrorCode.FORBIDDEN);

        commentDao.deleteComment(comment.getComment_id());
        if(comment.getParent_id()!=null){
            increaseReplyCount(comment.getParent_id(),-1,videoId);
        }
    }



    /**
     * 通用文件处理方法
     * @param filePath 文件路径
     * @param targetMap 目标Map
     * @param logPrefix 日志前缀
     */
    private <K, V> void processFile(Path filePath, Map<K, V> targetMap, String logPrefix){
        try {
            if (!Files.exists(filePath)) {
                return;
            }

            if(Files.size(filePath) <= 0) return;

            List<String> contents = Files.readAllLines(filePath);
            if (contents.isEmpty() || contents.get(0).isEmpty()) {
                return;
            }

            String content = contents.get(0);
            Map<K, V> dataMap = mapper.readValue(content, new TypeReference<Map<K, V>>() {});
            if(dataMap.isEmpty()) return;

            targetMap.clear();
            targetMap.putAll(dataMap);

            Files.write(filePath, "".getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (JsonProcessingException e) {
            logger.error("JSON parsing error for {}: {}", logPrefix, e.getMessage());
        } catch (IOException e) {
            logger.error("File operation error for {}: {}", logPrefix, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing {}: {}", logPrefix, e.getMessage());
        }
    }

    private String formatFileUrl(String originUrl,String expectationName){
        if(originUrl==null || originUrl.length() <1){
            return TEMP_DATA_DIR + expectationName;
        }
        return originUrl;
    }

    @Override
    public void run(String... args) {
        if(TEMP_DATA_DIR.isEmpty()){
            throw new RuntimeException("TEMP_DATA_DIR 目录不存在");
        }
        TEMP_DATA_VIDEO_COMMENTS_KEY_RECORD = formatFileUrl(TEMP_DATA_VIDEO_COMMENTS_KEY_RECORD,"VIDEO_COMMENTS_KEY_RECORD.data");
        TEMP_DATA_REPLY_COMMENTS_KEY_RECORD = formatFileUrl(TEMP_DATA_REPLY_COMMENTS_KEY_RECORD,"REPLY_COMMENTS_KEY_RECORD.data");
        TEMP_DATA_VIDEO_COMMENTS_RANK_RECORD = formatFileUrl(TEMP_DATA_VIDEO_COMMENTS_RANK_RECORD,"VIDEO_COMMENTS_RANK_RECORD.data");


        final Path videoCommentsKeyPath = Paths.get(TEMP_DATA_VIDEO_COMMENTS_KEY_RECORD);
        final Path replyCommentsKeyPath = Paths.get(TEMP_DATA_REPLY_COMMENTS_KEY_RECORD);
        final Path videoCommentsRankPath = Paths.get(TEMP_DATA_VIDEO_COMMENTS_RANK_RECORD);

        processFile(videoCommentsKeyPath, VIDEO_COMMENTS_KEY_RECORD, "VIDEO_COMMENTS_KEY_RECORD.data");
        processFile(replyCommentsKeyPath, REPLY_COMMENTS_KEY_RECORD, "REPLY_COMMENTS_KEY_RECORD.data");
        processFile(videoCommentsRankPath, VIDEO_COMMENTS_RANK_RECORD, "VIDEO_COMMENTS_RANK_RECORD.data");
    }



    private <k,v> void saveFile(Path filePath,ConcurrentHashMap<k,v> targetMap,String logPrefix){
        try{
            if(!Files.exists(filePath)) return;
            String str = mapper.writeValueAsString(targetMap);

            Files.write(filePath,str.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }catch (JsonProcessingException e) {
            logger.error("JSON string error for {}: {}", logPrefix, e.getMessage());
        } catch (IOException e) {
            logger.error("File wirte error for {}: {}", logPrefix, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing {}: {}", logPrefix, e.getMessage());
        }
    }

    @Override
    public void destroy() throws Exception {
        final Path videoCommentsKeyPath = Paths.get(TEMP_DATA_VIDEO_COMMENTS_KEY_RECORD);
        final Path replyCommentsKeyPath = Paths.get(TEMP_DATA_REPLY_COMMENTS_KEY_RECORD);
        final Path videoCommentsRankPath = Paths.get(TEMP_DATA_VIDEO_COMMENTS_RANK_RECORD);

        saveFile(videoCommentsKeyPath, VIDEO_COMMENTS_KEY_RECORD, "VIDEO_COMMENTS_KEY_RECORD.data");
        saveFile(replyCommentsKeyPath, REPLY_COMMENTS_KEY_RECORD, "REPLY_COMMENTS_KEY_RECORD.data");
        saveFile(videoCommentsRankPath, VIDEO_COMMENTS_RANK_RECORD, "VIDEO_COMMENTS_RANK_RECORD.data");
    }



    private void clearnComment(){
        long date = System.currentTimeMillis();
        Set<String> deleteKeys = new HashSet<>();

        if(!VIDEO_COMMENTS_KEY_RECORD.isEmpty()) {
            for (Map.Entry<String, Long> map : VIDEO_COMMENTS_KEY_RECORD.entrySet()) {
                if (map.getValue() <= date) {
                    deleteKeys.add(COMMENT_FOR_VIDEO_LIST_KEY(Integer.parseInt(map.getKey())));
                }
            }
        }

        if(!REPLY_COMMENTS_KEY_RECORD.isEmpty()){
            for(Map.Entry<String,Long> map : REPLY_COMMENTS_KEY_RECORD.entrySet()){
                if(map.getValue()<=date){
                    deleteKeys.add(COMMENT_FOR_ROOT_LIST_KEY(map.getKey()));
                }
            }
        }

        if(!VIDEO_COMMENTS_RANK_RECORD.isEmpty()){
            for(Map.Entry<String,Long> map : VIDEO_COMMENTS_RANK_RECORD.entrySet()){
                if(map.getValue() <= date) deleteKeys.add(COMMENT_SCORE_RANK(Integer.parseInt(map.getKey())));
            }
        }

        if(deleteKeys.isEmpty()) return;
        redis.delete(deleteKeys);
    }


    @Scheduled(cron = "0 0 0 * * ?")
    public void clearnCache(){
        final long allowMaxsizeHash = 1000;
        try {
            clearnComment();
        }catch (Exception e){
            logger.error("clearn cache fail :{}",e.getMessage());
        }

        Long commentStatsLen=redis.hLen(RedisKeyConstant.COMMENT_STATS_HASH_KEY);
        if(commentStatsLen==null || commentStatsLen <allowMaxsizeHash) return;

        redis.delete(RedisKeyConstant.COMMENT_STATS_HASH_KEY);
    }



    private void insertComment(){
        List<String> commentStr = redis.getRedisTemplate().execute(new SessionCallback<List<String>>() {
            @Override
            public List<String> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForList().range(RedisKeyConstant.COMMENT_INSERT_LIST_KEY,0,-1);
                operations.delete(RedisKeyConstant.COMMENT_INSERT_LIST_KEY);
                return (List<String>) operations.exec().get(0);
            }
        });

        if(commentStr==null || commentStr.isEmpty()) return;

        List<Comment> comments = new ArrayList<>();
        for(String str : commentStr){
            try{
                comments.add(mapper.readValue(str, mapper.constructType(Comment.class)));
            }catch (JacksonException jacksonException){

            }
        }

        if(comments.isEmpty()) return;

        try{
            commentDao.batchInsertComment(comments);
        }catch (Exception e){
            logger.error("batchInsertComment fail : {}",e.getMessage());
        }
    }

    private void insertACtion(){
        List<String> actions= redis.getRedisTemplate().execute(new SessionCallback<List<String>>() {
            @Override
            public List<String> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForList().range(RedisKeyConstant.COMMENT_ACTION_INSERT_LIST_KEY,0,-1);
                operations.delete(RedisKeyConstant.COMMENT_ACTION_INSERT_LIST_KEY);
                return (List<String>) (operations.exec().get(0));
            }
        });

        if (actions==null || actions.isEmpty()) return;

        List<CommentUserActions> userActions = new ArrayList<>();
        for(String action : actions){
            try{
                userActions.add(mapper.readValue(action,mapper.constructType(CommentUserActions.class)));
            }catch (JacksonException e){

            }
        }

        if(userActions.isEmpty()) return;

        try {
            commentDao.batchInsertAction(userActions);
        }catch (Exception e){
            logger.error("batchInsertAction fail : {}",e.getMessage());
        }
    }

    private void deleteAction(){
        List<String> actions= redis.getRedisTemplate().execute(new SessionCallback<List<String>>() {
            @Override
            public List<String> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForList().range(RedisKeyConstant.COMMENT_ACTION_DELETE_LIST_KEY,0,-1);
                operations.delete(RedisKeyConstant.COMMENT_ACTION_DELETE_LIST_KEY);
                return (List<String>) (operations.exec().get(0));
            }
        });

        if (actions==null || actions.isEmpty()) return;

        List<CommentUserActions> userActions = new ArrayList<>();
        for(String action : actions){
            try{
                userActions.add(mapper.readValue(action,mapper.constructType(CommentUserActions.class)));
            }catch (JacksonException e){

            }
        }

        if(userActions.isEmpty()) return;

        try {
            commentDao.batchDeleteAction(userActions);
        }catch (Exception e){
            logger.error("batchDeleteAction fail : {}",e.getMessage());
        }
    }

    @Scheduled(fixedRate = 5L*60*1000) //5分钟
    public void handleInsertAndDeleteTask(){
        insertComment();
        insertACtion();
        deleteAction();
    }


    @Scheduled(fixedRate = 4L*60*1000)//4分钟
    public void handleUpdateTask(){
        try {
            Map<String, Object> map = redis.getRedisTemplate().execute(new SessionCallback<Map<String, Object>>() {
                @Override
                public Map<String, Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.opsForHash().entries(RedisKeyConstant.COMMENT_COUNT_UPDATE_HASH_KEY);
                    operations.delete(RedisKeyConstant.COMMENT_COUNT_UPDATE_HASH_KEY);
                    return (Map<String, Object>) operations.exec().get(0);
                }
            });

            if (map == null || map.isEmpty()) return;
            List<CommentUpdate> updateTask = new ArrayList<>();
            String inval=RedisKeyConstant.INTERVAL_NUMBER;


            for(Map.Entry<String,Object> mapEntry : map.entrySet()){
                String[] key= mapEntry.getKey().split(inval);
                String commentId = key[0];
                String file = key[1];

                int count = Integer.parseInt(mapEntry.getValue().toString());

                CommentUpdate update= CommentUpdate.create(updateTask,commentId);
                if(COMMENT_LIKE_COUNT_FILE.equals(file)) update.setLikeCount(count);
                else if(COMMENT_REPLY_COUNT_FILE.equals(file)) update.setReplyCount(count);
                else continue;

                updateTask.add(update);
            }

            if(updateTask.isEmpty()) return;
            commentDao.batchUpdate(updateTask);

        }catch (Exception e){
            logger.error("handleUpdateTask fail : {}",e.getMessage(),e);
        }
    }

    public static class CommentUpdate{
        private String commentId;
        private Integer likeCount;
        private Integer replyCount;

        public CommentUpdate(){}
        private CommentUpdate(String commentId){
            this.commentId=commentId;
        }

        public static CommentUpdate create(List<CommentUpdate> commentUpdates,String commentId){
            for(CommentUpdate update : commentUpdates){
                if(update.commentId.equals(commentId)) return update;
            }

            return new CommentUpdate(commentId);
        }

        public Integer getLikeCount() {
            return likeCount;
        }

        public Integer getReplyCount() {
            return replyCount;
        }

        public String getCommentId() {
            return commentId;
        }

        public void setCommentId(String commentId) {
            this.commentId = commentId;
        }

        public void setLikeCount(Integer likeCount) {
            this.likeCount = likeCount;
        }

        public void setReplyCount(Integer replyCount) {
            this.replyCount = replyCount;
        }
    }
}