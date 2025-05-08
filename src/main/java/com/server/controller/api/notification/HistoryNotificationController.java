package com.server.controller.api.notification;


import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.push.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/history-notification")
@Tag(name = "历史通知记录")
public class HistoryNotificationController {
    @Autowired private NotificationService notificationService;

    private final Logger logger = LoggerFactory.getLogger(HistoryNotificationController.class);

    private int getUserId(HttpServletRequest request){
        return Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
    }

    @GetMapping("/like-video-notification/{offset}")
    @Operation(summary = "视频被点赞通知")
    public ResponseEntity<Result> getLikeVideoNotification(
            HttpServletRequest request,
            @PathVariable("offset") int offset
    ){
        try{
            int userId = getUserId(request);
            return Result.Ok(notificationService.getLikeVideoNotification(userId,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getLikeVideoNotification fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/like-comment-notification/{offset}")
    @Operation(summary = "评论被点赞通知")
    public ResponseEntity<Result> getLikeCommentNotification(
            HttpServletRequest request,
            @PathVariable("offset") int offset
    ){
        try{
            int userId = getUserId(request);
            return Result.Ok(notificationService.getLikeCommentNotification(userId,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getLikeCommentNotification fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/reply-comment-notification/{offset}")
    @Operation(summary = "回复评论通知")
    public ResponseEntity<Result> getReplyCommentNotification(
            HttpServletRequest request,
            @PathVariable("offset") int offset
    ){
        try{
            int userId = getUserId(request);
            return Result.Ok(notificationService.getReplyCommentNotification(userId,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getReplyCommentNotification fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/follow-notification/{offset}")
    @Operation(summary = "关注频道通知")
    public ResponseEntity<Result> getFollowNotification(
            HttpServletRequest request,
            @PathVariable("offset") int offset
    ){
        try{
            int userId = getUserId(request);
            return Result.Ok(notificationService.getFollowNotification(userId,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getFollowNotification fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
