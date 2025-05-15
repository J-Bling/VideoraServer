package com.server.service.commentservice.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.service.commentservice.comment.CommentDao;
import com.server.service.commentservice.comment.CommentUserActionDao;
import com.server.entity.cache.comment.CommentLockResponseDto;
import com.server.entity.cache.comment.CommentUserActionLockResponse;
import com.server.entity.cache.comment.CommentUserLockDto;
import com.server.dto.cache.StatsUpdateTask;
import com.server.dto.request.comment.CommentRequest;
import com.server.dto.request.comment.CommentUserActionRequest;
import com.server.dto.response.comment.CommentResponse;
import com.server.entity.constant.CommentConstant;
import com.server.entity.comment.Comment;
import com.server.entity.comment.CommentUserActions;
import com.server.entity.constant.RedisKeyConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.service.commentservice.CommentService;
import com.server.util.cache.CommentLockHasMapCache;
import com.server.util.cache.CommentUserActionLockHasMapCache;
import com.server.util.queue.TaskQueue;
import com.server.util.queue.UpdateQueue;
import com.server.util.redis.RedisUtil;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentDao commentDao;

    @Autowired
    private CommentUserActionDao commentUserActionDao;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisUtil redis;


    private final Logger logger= LoggerFactory.getLogger(CommentServiceImpl.class);
    private final ObjectMapper mapper =new ObjectMapper();

    private static final CommentLockHasMapCache COMMENT_RESPONSE_ROOT_LEVEL_CACHE=new CommentLockHasMapCache();
    private static final CommentLockHasMapCache COMMENT_RESPONSE_REPLY_LEVEL_CACHE=new CommentLockHasMapCache();
    private static final CommentUserActionLockHasMapCache COMMENT_USER_ACTION_CACHE=new CommentUserActionLockHasMapCache();

    private static final TaskQueue<Comment> COMMENT_INSERT_TASK_QUEUE=new TaskQueue<>();
    private static final TaskQueue<CommentUserActionRequest> COMMENT_USER_ACTION_INSERT_QUEUE=new TaskQueue<>();

    private static final UpdateQueue COMMENT_UPDATE_QUEUE=new UpdateQueue();
    private static final TaskQueue<CommentUserActionRequest> COMMENT_USER_ACTION_DELETE_QUEUE=new TaskQueue<>();
    private static final TaskQueue<Integer> COMMENT_USER_ACTION_CASCADE_DELETE_QUEUE=new TaskQueue<>();

    private static final int CACHE_CLEANUP_INTERVAL=30*60*1000;
    private static final int CLEANUP_INSERT_TASK_INTERVAL=60*1000;
    private static final int CLEANUP_DELETE_TASK_INTERVAL=45*1000;
    private static final int CLEANUP_UPDATE_TASK_INTERVAL=90*1000;
    private final int MAX_COMMENTS_COUNT_ON_CACHE=90;
    private static String COMMENT_KEY(Integer commentId){
        return RedisKeyConstant.COMMENT_KEY+commentId;
    }


    private boolean isValid(CommentRequest commentRequest){
        return commentRequest.getContent()!=null && commentRequest.getVideo_id()!=null;
    }


    private List<CommentResponse> filterCommentsByParentId(List<CommentResponse> comments,Integer parentId){
        if(comments==null || comments.isEmpty()){
            return comments;
        }

        List<CommentResponse> commentResponses=new ArrayList<>();

        for(CommentResponse comment : comments){
            if(comment.getParent_id().equals(parentId)){
                commentResponses.add(comment);
            }
        }

        return commentResponses;
    }

    private CommentResponse findCommentResponseByCache(Integer key,Integer targetId,boolean isRootLevel){
        List<CommentResponse> commentResponses= isRootLevel
                ? COMMENT_RESPONSE_ROOT_LEVEL_CACHE.getCommentResponse(key)
                : COMMENT_RESPONSE_REPLY_LEVEL_CACHE.getCommentResponse(key);

        if(commentResponses==null || commentResponses.isEmpty()){
            return null;
        }

        for(CommentResponse commentResponse : commentResponses){
            if(commentResponse.getId().equals(targetId)){
                return commentResponse;
            }
        }

        return null;
    }

    public List<CommentResponse> findCommentsByOffsetWithVideo
            (CommentLockResponseDto commentLockResponseDto,int videoId,int offset,int limit)
    {
        List<CommentResponse> commentResponses  =commentLockResponseDto.getCache();
        if(commentResponses.size()<=offset){
            commentResponses = this.commentDao.findPublicVideoComments(videoId,offset,limit);

            if((offset<=MAX_COMMENTS_COUNT_ON_CACHE)
                    && commentResponses!=null
                    && !commentResponses.isEmpty()
            ){
                commentLockResponseDto.addCache(commentResponses);
            }
        }

        return commentResponses;
    }

    public List<CommentResponse> findCommentsByOffsetWithRoot
            (CommentLockResponseDto commentLockResponseDto,int rootId,int parentId,int offset,int limit)
    {
        List<CommentResponse> commentResponses  =commentLockResponseDto.getCache();
        commentResponses=this.filterCommentsByParentId(commentResponses,parentId);

        if(commentResponses.size()<=offset){
            commentResponses = this.commentDao.findPublicReplyComments(rootId,parentId,offset,limit);

            if((offset<=MAX_COMMENTS_COUNT_ON_CACHE)
                    && commentResponses!=null
                    && !commentResponses.isEmpty()
            ){
                commentLockResponseDto.addCache(commentResponses);
            }
        }

        return commentResponses;
    }


    private Boolean matchCommentAction(List<CommentUserActions> actions,Integer commentId){
        if(actions==null) return null;
        for(CommentUserActions actions1 : actions){
            if(actions1.getComment_id().equals(commentId)){
                return actions1.getAction_type();
            }
        }
        return null;
    }

    private List<CommentResponse> fillCommentUserActionCache(int userId,List<CommentResponse> responses){
        if(responses==null || responses.isEmpty()){
            return null;
        }

        List<CommentUserActions> actions=this.commentUserActionDao.findActionList(userId,responses);
        for (CommentResponse response : responses){
            CommentUserLockDto lockDto =COMMENT_USER_ACTION_CACHE.getCommentUserLock(response.getId());

            Boolean type=this.matchCommentAction(actions,response.getId());
            lockDto.add(new CommentUserActionLockResponse(
                    userId,type
            ));
            response.setAction(type);
        }

        return responses;
    }

    private List<CommentResponse> handleCommentResponseAction(List<CommentResponse> responses,int userId){
        if(responses==null || responses.isEmpty()){
            return null;
        }

        List<CommentResponse> missedTarget=new ArrayList<>();
        List<CommentResponse> hitTarget=new ArrayList<>();

        for(CommentResponse response : responses){
            CommentUserLockDto lockDto= COMMENT_USER_ACTION_CACHE.getCommentUserLock(response.getId());

            if(!lockDto.getActions().isEmpty() && lockDto.contains(userId)){
                CommentUserActionLockResponse lockResponse=lockDto.find(userId);
                response.setAction(lockResponse==null ? null : lockResponse.getAction_type());
                hitTarget.add(response);
                continue;
            }

            missedTarget.add(response);
        }

        List<CommentResponse> hitComments=this.fillCommentUserActionCache(userId,missedTarget);
        if(hitComments!=null && !hitComments.isEmpty()) hitTarget.addAll(hitComments);

        return hitTarget;
    }

/**
private CommentUserActionLockResponse findCommentUserAction(Integer commentId,int userId){
    CommentUserLockDto lockDto=COMMENT_USER_ACTION_CACHE.getCommentUserLock(commentId);
    CommentUserActionLockResponse lockResponse =lockDto.find(userId);

    if(lockResponse==null){
        synchronized (lockDto.getLOCK()){
            lockResponse =lockDto.find(userId);
            if(lockResponse!=null) return lockResponse;

            CommentUserActions action=this.commentUserActionDao.findAction(commentId,userId);

            lockResponse=action!=null
                    ? new CommentUserActionLockResponse(userId,action.getAction_type())
                    : new CommentUserActionLockResponse(userId,null);

            lockDto.add(lockResponse,1);
            return lockResponse;
        }

    }
    return lockResponse;
}
 */

    private Boolean isVailActionRequest(CommentUserActionLockResponse lockResponse,CommentUserActionRequest request){
        if(lockResponse.getAction_type()==request.getAction_type()) throw new ApiException(ErrorCode.BAD_REQUEST);

        return lockResponse.getAction_type();
    }

    private Boolean handleCommentActionCache(CommentUserActionRequest request){

        CommentUserLockDto lockDto=COMMENT_USER_ACTION_CACHE.getCommentUserLock(request.getComment_id());
        CommentUserActionLockResponse lockResponse =lockDto.find(request.getUser_id());

        if(lockResponse==null){
            synchronized (lockDto.getLOCK()){
                lockResponse =lockDto.find(request.getUser_id());
                if(lockResponse!=null) return isVailActionRequest(lockResponse,request);
                CommentUserActions action=
                        this.commentUserActionDao.findAction(request.getComment_id(), request.getUser_id());

                lockResponse=action!=null
                        ? new CommentUserActionLockResponse(request.getUser_id(),action.getAction_type())
                        : new CommentUserActionLockResponse(request.getUser_id(), null);

                lockResponse.setAction_type(request.getAction_type());
                lockDto.add(lockResponse,1);//add(param) 有锁不能在被锁占了使用

                if(request.getAction_type()==(action==null ? null : action.getAction_type())){
                    throw new ApiException(ErrorCode.BAD_REQUEST);
                }

                return action==null?null : action.getAction_type();
            }

        }

        return isVailActionRequest(lockResponse,request);
    }



    /**
     *   缓存按需查询
     *   先查询缓存有没有对应的videoId 的评论数据 没有就数据库，有但是不够就去数据库找
     */
    @Override
    public List<CommentResponse> getVideoComments(int videoId, int userId,
                                                  int offset, int limit, boolean isHot){
        if(isHot){
            CommentLockResponseDto commentLockResponseDto = COMMENT_RESPONSE_ROOT_LEVEL_CACHE.getLockComment(videoId);
            List<CommentResponse> commentResponses=commentLockResponseDto.getCache();

            if(commentResponses.isEmpty()){
                synchronized (commentLockResponseDto.getLOCK()){
                    commentResponses=commentLockResponseDto.getCache();
                    if(commentResponses.isEmpty()){
                        commentResponses=this.commentDao.findPublicVideoComments(videoId,offset,limit);

                        if((offset<MAX_COMMENTS_COUNT_ON_CACHE)
                                && commentResponses !=null
                                && !commentResponses.isEmpty())
                        {
                            commentLockResponseDto.setCache(commentResponses);
                        }
                    }else if(commentResponses.size()<=offset){
                        commentResponses=
                                this.findCommentsByOffsetWithVideo(commentLockResponseDto,videoId,offset,limit);
                    }

                    return commentResponses==null ? null
                            : this.handleCommentResponseAction(commentResponses,userId);
                }
            }

            if(commentResponses.size()<=offset){
                synchronized (commentLockResponseDto.getLOCK()){
                    return this.handleCommentResponseAction(
                            this.findCommentsByOffsetWithVideo(commentLockResponseDto,videoId,offset,limit),
                            userId
                    );
                }
            }

            return this.handleCommentResponseAction(
                    commentResponses.subList(offset,Math.min(limit,commentResponses.size()))
                    ,userId);
        }

        return this.commentDao.findVideoCommentsByVideoIdOnNew(videoId,userId,offset,limit);
    }


    @Override
    public List<CommentResponse> getReplyComments(int rootId,int parentId,int userId,
                                                  int offset,int limit,boolean isHot){
        if(!isHot){
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        if(rootId==0 || parentId==0){
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        CommentLockResponseDto commentLockResponseDto = COMMENT_RESPONSE_REPLY_LEVEL_CACHE.getLockComment(rootId);
        List<CommentResponse> commentReplies = commentLockResponseDto.getCache();

        if(commentReplies.isEmpty()){
            synchronized (commentLockResponseDto.getLOCK()){
                commentReplies = commentLockResponseDto.getCache();
                if(commentReplies.isEmpty()){
                    commentReplies=this.commentDao.findPublicReplyComments(rootId,parentId,offset,limit);

                    if((offset<=MAX_COMMENTS_COUNT_ON_CACHE)
                            && commentReplies!=null
                            && !commentReplies.isEmpty()
                    ){
                        commentLockResponseDto.setCache(commentReplies);
                    }
                }else if(commentReplies.size()<=offset){
                    commentReplies=
                            this.findCommentsByOffsetWithRoot(commentLockResponseDto,rootId,parentId,offset,limit);
                }

                return this.handleCommentResponseAction(commentReplies,userId);
            }
        }

        //需要按 parentId来分级筛选
        List<CommentResponse> commentResponsesList=this.filterCommentsByParentId(commentReplies,parentId);

        if(commentResponsesList.size()<=offset){
            synchronized (commentLockResponseDto.getLOCK()) {
                commentReplies=this.findCommentsByOffsetWithRoot(commentLockResponseDto,rootId,parentId,offset,limit);
                return this.handleCommentResponseAction(commentReplies,userId);
            }
        }

        return this.handleCommentResponseAction(
                commentResponsesList.subList(offset,Math.min(limit,commentReplies.size()))
                ,userId);
    }


    @Override
    public List<CommentResponse> getPublicVideoComments(int videoId,int offset,int limit){
        CommentLockResponseDto commentLockResponseDto = COMMENT_RESPONSE_ROOT_LEVEL_CACHE.getLockComment(videoId);
        List<CommentResponse> commentResponses  =commentLockResponseDto.getCache();

        if(commentResponses.isEmpty()){
            synchronized (commentLockResponseDto.getLOCK()) {
                commentResponses = commentLockResponseDto.getCache();

                if (commentResponses.isEmpty()) {
                    commentResponses = this.commentDao.findPublicVideoComments(videoId, offset, limit);
                    if ((offset<=MAX_COMMENTS_COUNT_ON_CACHE) && !commentResponses.isEmpty()) {
                        commentLockResponseDto.setCache(commentResponses);
                    }
                }else if(commentResponses.size()<=offset){
                    commentResponses=this.findCommentsByOffsetWithVideo(commentLockResponseDto,videoId,offset,limit);
                }

                return commentResponses;
            }
        }

        if(commentResponses.size()<=offset){
            synchronized (commentLockResponseDto.getLOCK()){
                return this.findCommentsByOffsetWithVideo(commentLockResponseDto,videoId,offset,limit);
            }
        }

        return commentResponses.subList(offset,Math.min(limit,commentResponses.size()));
    }


    @Override
    public List<CommentResponse> getPublicReplyComments(int rootId,int parentId,int offset,int limit){
        if(rootId==0 || parentId==0){
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        CommentLockResponseDto commentLockResponseDto =COMMENT_RESPONSE_REPLY_LEVEL_CACHE.getLockComment(rootId);
        List<CommentResponse> commentResponses =commentLockResponseDto.getCache();

        if(commentResponses.isEmpty()){
            synchronized (commentLockResponseDto.getLOCK()) {
                commentResponses =commentLockResponseDto.getCache();

                if(commentResponses.isEmpty()){
                    commentResponses = this.commentDao.findPublicReplyComments(rootId, parentId, offset, limit);

                    if ((offset<=MAX_COMMENTS_COUNT_ON_CACHE) && commentResponses != null) {
                        commentLockResponseDto.setCache(commentResponses);
                    }

                }else if(commentResponses.size()<=offset){
                    commentResponses=
                            this.findCommentsByOffsetWithRoot(commentLockResponseDto,rootId,parentId,offset,limit);
                }

                return commentResponses;
            }
        }

        List<CommentResponse> comments=this.filterCommentsByParentId(commentResponses,parentId);

        if(comments.size()<=offset){
            synchronized (commentLockResponseDto.getLOCK()) {
                return this.findCommentsByOffsetWithRoot(commentLockResponseDto,rootId,parentId,offset,limit);
            }
        }

        return comments;
    }

    @Override
    public CommentResponse getPublicCommentByRedis(Integer commentId) {
        String commentStr = redis.get(COMMENT_KEY(commentId));
        if(commentStr==null){
            CommentResponse comment= commentDao.findPublicComment(commentId);
            if(comment==null) return null;
            try{
                commentStr=mapper.writeValueAsString(CommentResponse.toCommentCacheResponse(comment));
                redis.set(COMMENT_KEY(commentId),commentStr,RedisKeyConstant.CLEAN_CACHE_SPACED);
            }catch (JsonProcessingException e){
                logger.error("comment序列化失败 : {}",e.getMessage());
            }
            return comment;
        }

        try{
            return mapper.readValue(commentStr,mapper.constructType(CommentResponse.class));
        }catch (JsonProcessingException e){
            logger.error("comment反序列化失败 : {}",e.getMessage());
        }

        return null;
    }

    @Override
    public List<CommentResponse> getPublicCommentsByRedis(Integer[] commentIds) {
        List<CommentResponse> commentResponses = new ArrayList<>();
        if(commentIds.length < 500){
            for(Integer id : commentIds){
                commentResponses.add(getPublicCommentByRedis(id));
            }

            return commentResponses;
        }

        Set<String> ids = new HashSet<>();
        for(Integer id : commentIds){
            ids.add(id.toString());
        }

        List<Object> commentsStr = redis.mGet(ids);
        int i=0;
        for(Object commentObj : commentsStr){
            if(commentObj==null) {
                commentResponses.set(i,getPublicCommentByRedis(commentIds[i]));
                continue;
            }
            try{
                CommentResponse comment = mapper.readValue(commentObj.toString(),mapper.constructType(CommentResponse.class));
                commentResponses.set(i,comment);
            }catch (JsonProcessingException e){
                commentResponses.set(i,null);
                logger.error("comment反序列化失败 : {}",e.getMessage());
            }finally {
                i++;
            }
        }

        return commentResponses;
    }


    @Override
    public void handleAction(CommentUserActionRequest request){
        request.vailColumn();

        CommentResponse comment = request.getRootId()==0
                ? this.findCommentResponseByCache(request.getVideoId(),request.getComment_id(),true)
                : this.findCommentResponseByCache(request.getRootId(),request.getComment_id(),false);
        Boolean lastAction= this.handleCommentActionCache(request);

        if(request.getAction_type()==null){
            if(comment!=null){
                if(Boolean.TRUE.equals(lastAction)) comment.updateLikeCount(-1);
                else comment.updateDisLikeCount(-1);
            }

            COMMENT_UPDATE_QUEUE.setUpdateTask(
                    StatsUpdateTask.forComment(
                            request.getComment_id(),
                            Boolean.TRUE.equals(lastAction)
                                    ? CommentConstant.COMMENT_LIKE_COUNT
                                    : CommentConstant.COMMENT_DISLIKE_COUNT,
                            -1
                    )
            );
        COMMENT_USER_ACTION_DELETE_QUEUE.setTask(
                new CommentUserActionRequest(request.getComment_id(),request.getUser_id(),request.getAction_type())
        );

        }else{
            if(request.getAction_type()) {
                if (lastAction != null && !lastAction) {
                    COMMENT_UPDATE_QUEUE.setUpdateTask(
                            StatsUpdateTask.forComment(
                                    request.getComment_id(),
                                    CommentConstant.COMMENT_DISLIKE_COUNT,
                                    -1
                            )
                    );

                    if (comment != null) comment.updateDisLikeCount(-1);
                }

                if (comment != null) comment.updateLikeCount(1);

                COMMENT_UPDATE_QUEUE.setUpdateTask(
                        StatsUpdateTask.forComment(
                                request.getComment_id(),
                                CommentConstant.COMMENT_LIKE_COUNT,
                                1
                        )
                );

            }else {
                if(lastAction!=null && lastAction){
                    COMMENT_UPDATE_QUEUE.setUpdateTask(
                            StatsUpdateTask.forComment(
                                    request.getComment_id(),
                                    CommentConstant.COMMENT_LIKE_COUNT,
                                    -1
                            )
                    );

                    if (comment != null) comment.updateLikeCount(-1);
                }

                COMMENT_UPDATE_QUEUE.setUpdateTask(
                        StatsUpdateTask.forComment(
                                request.getComment_id(),
                                CommentConstant.COMMENT_DISLIKE_COUNT,
                                1
                        )
                );
            }

            COMMENT_USER_ACTION_INSERT_QUEUE.setTask(
                    new CommentUserActionRequest(
                            request.getComment_id(),request.getUser_id(),request.getAction_type()
                    )
            );

        }
    }

    /**
//    @Override
//    public void handleAction(CommentUserActionRequest request){
//        request.vailColumn();
//
//        CommentUserActionLockResponse actionLockResponse =
//                this.findCommentUserAction(request.getComment_id(), request.getUser_id());
//
//        CommentResponse comment= request.getRootId()==0
//                ? this.findCommentResponseByCache(request.getVideoId(),request.getComment_id(),true)
//                : this.findCommentResponseByCache(request.getRootId(),request.getComment_id(),false);
//
//
//        if(request.getAction_type() ==null && request.getLastAction() !=null){
//            //取消时 操作过的数据不可能没有记录的 要注意
//            if(actionLockResponse==null || actionLockResponse.getAction_type()==null){
//                throw new ApiException(ErrorCode.NOT_FOUND);
//            }
//
//            if(actionLockResponse.getAction_type()){
//                COMMENT_UPDATE_QUEUE.setUpdateTask(
//                        StatsUpdateTask.forComment(
//                                request.getComment_id(),
//                                Constant.COMMENT_LIKE_COUNT,
//                                -1
//                        )
//                );
//
//                if(comment!=null){
//                    comment.updateLikeCount(-1);
//                }
//
//            }else {
//                COMMENT_UPDATE_QUEUE.setUpdateTask(
//                        StatsUpdateTask.forComment(
//                                request.getComment_id(),
//                                Constant.COMMENT_DISLIKE_COUNT,
//                                -1
//                        )
//                );
//
//                if(comment!=null){
//                    comment.updateDisLikeCount(-1);
//                }
//            }
//
//            actionLockResponse.updateActionType(request.getAction_type());//缓存修改
//            COMMENT_USER_ACTION_DELETE_QUEUE.setTask(request);//进入删除队列等待
//            return;
//        }
//
//        if(request.getAction_type()!=null && request.getAction_type()){
//            //点赞时
//            if(actionLockResponse!=null){
//                //有过记录
//                if(actionLockResponse.getAction_type()!=null && actionLockResponse.getAction_type()){
//                    throw new ApiException(ErrorCode.BAD_REQUEST);
//                }
//
//                if(actionLockResponse.getAction_type()!=null){
//                    COMMENT_UPDATE_QUEUE.setUpdateTask(StatsUpdateTask.forComment(
//                            request.getComment_id(),
//                            Constant.COMMENT_DISLIKE_COUNT,
//                            -1
//                    ));
//
//                    if(comment!=null){
//                        comment.updateDisLikeCount(-1);
//                    }
//                }
//
//                COMMENT_UPDATE_QUEUE.setUpdateTask(
//                        StatsUpdateTask.forComment(
//                                request.getComment_id(),
//                                Constant.COMMENT_LIKE_COUNT,
//                                1
//                        )
//                );
//
//                COMMENT_USER_ACTION_INSERT_QUEUE.setTask(request);//进入插入/更新队列
//
//                actionLockResponse.updateActionType(request.getAction_type());//修改缓存
//
//
//            }else {
//                //没有任何记录
//                COMMENT_UPDATE_QUEUE.setUpdateTask(
//                        StatsUpdateTask.forComment(
//                                request.getComment_id(),
//                                Constant.COMMENT_LIKE_COUNT,
//                                1
//                        )
//                );//comment被点赞数修改
//
//                COMMENT_USER_ACTION_INSERT_QUEUE.setTask(request);//进入插入/更新队列
//
//                actionLockResponse=new CommentUserActionLockResponse(
//                        request.getUser_id(),true
//                );
//
//                COMMENT_USER_ACTION_CACHE.putCache(actionLockResponse,request.getComment_id());//进入缓存
//            }
//
//            if(comment!=null){
//                comment.updateLikeCount(1);
//            }
//
//            return;
//        }
//
//        if(request.getAction_type()!=null && !request.getAction_type()){
//            //点踩
//
//            if(actionLockResponse==null){
//                COMMENT_UPDATE_QUEUE.setUpdateTask(
//                        StatsUpdateTask.forComment(
//                                request.getComment_id(),
//                                Constant.COMMENT_DISLIKE_COUNT,
//                                1
//                        )
//                );//comment被点赞数修改
//
//                COMMENT_USER_ACTION_INSERT_QUEUE.setTask(request);
//
//                actionLockResponse=new CommentUserActionLockResponse(
//                        request.getUser_id(),
//                        false
//                );
//
//                COMMENT_USER_ACTION_CACHE.putCache(actionLockResponse,request.getComment_id());
//
//            }else {
//                if(actionLockResponse.getAction_type()!=null && !actionLockResponse.getAction_type()){
//                    throw new ApiException(ErrorCode.BAD_REQUEST);
//                }
//
//                if(actionLockResponse.getAction_type() != null){
//                    COMMENT_UPDATE_QUEUE.setUpdateTask(
//                            StatsUpdateTask.forComment(
//                                    request.getComment_id(),
//                                    Constant.COMMENT_LIKE_COUNT,
//                                    -1
//                            )
//                    );
//
//                    if(comment!=null){
//                        comment.updateLikeCount(-1);
//                    }
//                }
//
//                COMMENT_UPDATE_QUEUE.setUpdateTask(
//                        StatsUpdateTask.forComment(
//                                request.getComment_id(),
//                                Constant.COMMENT_DISLIKE_COUNT,
//                                1
//                        )
//                );
//
//                COMMENT_USER_ACTION_INSERT_QUEUE.setTask(request);
//                actionLockResponse.updateActionType(false);
//            }
//            if(comment!=null){
//                comment.updateDisLikeCount(1);
//            }
//            return;
//        }
//
//        throw new ApiException(ErrorCode.BAD_REQUEST);
//    }
     */


    @Override
    public void createComment(CommentRequest commentRequest,int userId){
        if(!this.isValid(commentRequest)) {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        commentRequest.setRoot_id(
                (commentRequest.getRoot_id()!=null
                ? commentRequest.getRoot_id()
                : 0)
        );
        commentRequest.setParent_id(
                (commentRequest.getParent_id()!=null
                ? commentRequest.getParent_id()
                : 0)
        );

        Comment comment=new Comment(commentRequest);
        comment.setUser_id(userId);

        COMMENT_INSERT_TASK_QUEUE.setTask(comment);

        if(comment.getRoot_id()!=0 && comment.getParent_id()!=0){
            CommentResponse commentResponse =
                    this.findCommentResponseByCache(
                            comment.getRoot_id(),
                            comment.getId(),
                            false
                    );
            if(commentResponse!=null){

                commentResponse.updateReplyCount(1);
            }

            COMMENT_UPDATE_QUEUE.setUpdateTask(
                    StatsUpdateTask.forComment(comment.getParent_id(), CommentConstant.COMMENT_REPLY_COUNT,1)
            );
        }

    }


    @Override
    @Transactional(value = "mysqlTransactionManager")
    public void deleteComment(int commentId,int videoId,int rootId,int parentId,int userId){
        int status= this.commentDao.deleteComment(commentId,userId);
        if(status<=0){
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        CommentLockResponseDto responseDto = rootId ==0
                ? COMMENT_RESPONSE_ROOT_LEVEL_CACHE.getLockComment(videoId)
                : COMMENT_RESPONSE_REPLY_LEVEL_CACHE.getLockComment(rootId);


        if(responseDto!=null){
            synchronized (responseDto.getLOCK()){
                responseDto.remove(commentId);
            }
        }

        if(parentId>0) COMMENT_UPDATE_QUEUE.setUpdateTask(
                StatsUpdateTask.forComment(parentId, CommentConstant.COMMENT_REPLY_COUNT,-1)
        );

        COMMENT_USER_ACTION_CASCADE_DELETE_QUEUE.setTask(commentId);
        //当一条评论删除后删除其他与他关联的数据
    };


    @Scheduled(fixedRate = CACHE_CLEANUP_INTERVAL)
    public void cleanCache(){
        COMMENT_USER_ACTION_CACHE.cleanCache();
        COMMENT_RESPONSE_ROOT_LEVEL_CACHE.cleanCache();
        COMMENT_RESPONSE_REPLY_LEVEL_CACHE.cleanCache();
    }

    @Scheduled(fixedRate = CLEANUP_INSERT_TASK_INTERVAL)
    public void cleanInsertTask() throws SQLException {
        List<Comment> comments =COMMENT_INSERT_TASK_QUEUE.cleanQueue();
        List<CommentUserActionRequest> commentUserActionRequests =COMMENT_USER_ACTION_INSERT_QUEUE.cleanQueue();

        if(comments!=null && !comments.isEmpty()){
            String insertCommentSql="insert into comment (video_id,user_id,root_id,parent_id,content) values (?,?,?,?,?)";

            try(Connection connection=dataSource.getConnection()){
                int batchSize = 500;
                int count = 0;
                connection.setAutoCommit(false);
                try(PreparedStatement ps=connection.prepareCall(insertCommentSql)){
                    for(Comment comment : comments){
                        ps.setInt(1,comment.getVideo_id());
                        ps.setInt(2,comment.getUser_id());
                        ps.setInt(3,comment.getRoot_id());
                        ps.setInt(4,comment.getParent_id());
                        ps.setString(5,comment.getContent());
                        ps.addBatch();
                        if(++count % batchSize==0){
                            ps.executeBatch();
                            connection.commit();
                        }
                    }
                    ps.executeBatch();
                    connection.commit();
                }
            }
        }

        if(commentUserActionRequests!=null && !commentUserActionRequests.isEmpty()){
            String insertCommentUserActionSql="insert into comment_user_actions (comment_id,user_id,action_type) values (?,?,?) " +
                    "on duplicate key update comment_id=values(comment_id),user_id=values(user_id),action_type=values(action_type)";

            try(Connection connection=dataSource.getConnection()){
                int batchSize = 500;
                int count = 0;
                connection.setAutoCommit(false);
                try(PreparedStatement ps=connection.prepareCall(insertCommentUserActionSql)){
                    for(CommentUserActionRequest commentUserActionRequest : commentUserActionRequests){
                        ps.setInt(1,commentUserActionRequest.getComment_id());
                        ps.setInt(2,commentUserActionRequest.getUser_id());
                        ps.setBoolean(3,commentUserActionRequest.getAction_type());
                        ps.addBatch();
                        if(++count % batchSize==0){
                            ps.executeBatch();
                            connection.commit();
                        }
                    }
                    ps.executeBatch();
                    connection.commit();
                }
            }
        }
    }

    private StatsUpdateTask contain(List<StatsUpdateTask> tasks,StatsUpdateTask statsUpdateTask){
        if(tasks==null || tasks.isEmpty()) return null;
        for(StatsUpdateTask task : tasks){
            if(task.getTargetId().equals(statsUpdateTask.getTargetId())) return task;
        }
        return null;
    }

    @Scheduled(fixedRate = CLEANUP_UPDATE_TASK_INTERVAL)
    @Transactional(value = "mysqlTransactionManager")
    public void cleanUpdateTask(){
        List<StatsUpdateTask> statsUpdateTasks =COMMENT_UPDATE_QUEUE.cleanQueue();
        if(statsUpdateTasks==null || statsUpdateTasks.isEmpty()){
            return;
        }


        Map<String,List<StatsUpdateTask>> tasksByColumn =new HashMap<>();
        for(StatsUpdateTask task : statsUpdateTasks){
            tasksByColumn.computeIfAbsent(task.getColumn(),k->new ArrayList<>()).add(task);
        }

        for(Map.Entry<String,List<StatsUpdateTask>> entry : tasksByColumn.entrySet()) {
            String column = entry.getKey();
            List<StatsUpdateTask> tasks = entry.getValue();

            List<StatsUpdateTask> updateTasks =new ArrayList<>();
            for(StatsUpdateTask task : tasks){
                StatsUpdateTask statsUpdateTask= this.contain(updateTasks,task);
                if(statsUpdateTask==null){
                    updateTasks.add(task);
                }else {
                    statsUpdateTask.addCount(task.getCount());
                }
            }

            try{
                if(updateTasks.isEmpty()) continue;
                for(int i=0;i<updateTasks.size();i+=500){
                    int end=Math.min(i+500,updateTasks.size());
                    this.commentDao.updateBatchComments(column,updateTasks.subList(i,end));
                }

            }catch (Exception e){
                logger.error("批量更新 comment 表 失败  字段 {}  ,失败原因 {}",column,e.getMessage(),e);
            }
        }
    }

    @Scheduled(fixedRate = CLEANUP_DELETE_TASK_INTERVAL)
    @Transactional(value = "mysqlTransactionManager")
    public void cleanDeleteTask() throws SQLException {
        List<CommentUserActionRequest> commentUserActionRequests = COMMENT_USER_ACTION_DELETE_QUEUE.cleanQueue();
        if(commentUserActionRequests!=null && !commentUserActionRequests.isEmpty()){
            String deleteCommentUserActionSql="delete from comment_user_actions where user_id=? and comment_id=?";

            try(Connection connection = dataSource.getConnection()){
                int batchSize = 500;
                int count = 0;
                connection.setAutoCommit(false);
                try(PreparedStatement ps =connection.prepareCall(deleteCommentUserActionSql)){
                    for(CommentUserActionRequest commentUserActionRequest : commentUserActionRequests){
                        ps.setInt(1,commentUserActionRequest.getUser_id());
                        ps.setInt(2,commentUserActionRequest.getComment_id());
                        ps.addBatch();
                        if(++count % batchSize ==0){
                            ps.executeBatch();
                            connection.commit();
                        }
                    }
                    ps.executeBatch();
                    connection.commit();
                }
            }
        }

        List<Integer> commentIds = COMMENT_USER_ACTION_CASCADE_DELETE_QUEUE.cleanQueue();
        if(commentIds==null || commentIds.isEmpty()) return;

        try {
            for (int i = 0; i < commentIds.size(); i += 500) {
                int end = Math.min(i + 500, commentIds.size());
                this.commentUserActionDao.deleteBatchCascade(commentIds.subList(i, end));
            }
        }catch (Exception e){
            logger.error("批量 级联删除 comment_user_action 失败 , 原因 : {}",e.getMessage(),e);
        }
    }

}