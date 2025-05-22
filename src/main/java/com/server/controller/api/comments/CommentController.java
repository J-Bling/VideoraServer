package com.server.controller.api.comments;

import com.server.comment.dto.request.CreateCommentRequest;
import com.server.comment.server.CommentService;
import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.push.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/comments")
@Tag(name = "comment 访问 创建 互动")
public class CommentController {
    @Autowired private CommentService commentService;
    @Autowired private NotificationService notificationService;

    private final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private Integer getUserId(HttpServletRequest request){
        return Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
    }

    @PostMapping("/create")
    public ResponseEntity<Result> createComment(HttpServletRequest request, @RequestBody CreateCommentRequest commentRequest){
        try{
            Integer userId = this.getUserId(request);
            String commentId = commentService.createComment(commentRequest,userId);
            if(commentRequest.getTargetId()!=null && !userId.equals(commentRequest.getTargetId())) {
                notificationService.commentToCommentNotices(userId,commentRequest.getTargetId(),commentId);
            }
            return Result.Ok(commentId);
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/video-comments/{videoId}/{offset}")
    public ResponseEntity<Result> getCommentsByVideoId(
            HttpServletRequest request,@PathVariable("videoId") int videoId,
            @PathVariable("offset") int offset
    ){
        try{
            Integer userId = getUserId(request);
            return Result.Ok(commentService.getCommentsByVideoId(videoId,offset,userId));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/video-comments-hot/{videoId}/{offset}")
    public ResponseEntity<Result> getCommentByVideoWithScore(HttpServletRequest request,@PathVariable("videoId") int videoId,
                                                             @PathVariable("offset") int offset){
        try{
            Integer userId = getUserId(request);
            return Result.Ok(commentService.getCommentByVideoWithScore(videoId,userId,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/reply-comments/{videoId}/{rootId}/{offset}")
    public ResponseEntity<Result> getReplyByVideoId(HttpServletRequest request,
                                                    @PathVariable("videoId") int videoId,
                                                    @PathVariable("rootId") String rootId,
                                                    @PathVariable("offset") int offset){
        try{
            return Result.Ok(commentService.getReplyByVideoId(videoId,rootId,getUserId(request),offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/like/{commentId}/{videoId}/{targetId}")
    public ResponseEntity<Result> like(
            HttpServletRequest request,
            @PathVariable("commentId") String commentId,
            @PathVariable("videoId") int videoId,
            @PathVariable("targetId") Integer targetId
    )
    {
        try {
                if(commentId==null||commentId.equals("undefined")||targetId==null) return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);
                Integer userId = getUserId(request);
                boolean status =commentService.like(commentId,userId,videoId);
                if(status) {
                    if(!userId.equals(targetId)) notificationService.likeToCommentNotices(userId,targetId,commentId);
                    return Result.Ok(1);
                }else {
                    return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);
                }

        } catch (ApiException apiException) {
            return Result.ErrorResult(apiException.getErrorCode(), 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR, 0);
        }
    }

    @GetMapping("/dislike/{commentId}/{videoId}")
    public ResponseEntity<Result> dislike(HttpServletRequest request,
                                          @PathVariable("commentId") String commentId,
                                          @PathVariable("videoId") int videoId) {
        try {
            if(commentId==null||commentId.equals("undefined")) return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);
            return commentService.disLike(commentId, getUserId(request), videoId)
                    ? Result.Ok(1)
                    : Result.ErrorResult(ErrorCode.BAD_REQUEST, 0);
        } catch (ApiException apiException) {
            return Result.ErrorResult(apiException.getErrorCode(), 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR, 0);
        }
    }


    @DeleteMapping("/delete/{commentId}/{videoId}")
    public ResponseEntity<Result> deleteComment(HttpServletRequest request,
                                                @PathVariable("commentI") String commentId,
                                                @PathVariable("videoId") int videoId){
        try{
            if (commentId == null) return Result.ErrorResult(ErrorCode.BAD_REQUEST, 0);
            commentService.deleteComment(commentId,getUserId(request),videoId);
            return Result.Ok(1);
        }catch (ApiException apiException) {
            return Result.ErrorResult(apiException.getErrorCode(), 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR, 0);
        }
    }
}
