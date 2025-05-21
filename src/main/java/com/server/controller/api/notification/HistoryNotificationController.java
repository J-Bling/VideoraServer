package com.server.controller.api.notification;


import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.push.enums.NotificationCode;
import com.server.push.service.NotificationService;
import com.server.push.service.impl.NotificationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    public  List<NotificationServiceImpl.NotificationReplyResponse> getLikeCommentNotification(
            HttpServletRequest request,
            @PathVariable("offset") int offset
    ){
        try{
            int userId = getUserId(request);
            return notificationService.getLikeCommentNotification(userId,offset);
        }catch (ApiException apiException){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,apiException.getErrorCode().getStandardMessage());
        }catch (Exception e){
            logger.error("getLikeCommentNotification fail reason is : {}",e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"服务器错误");
        }
    }

    @GetMapping("/reply-comment-notification/{offset}")
    @Operation(summary = "回复评论通知")
    public List<NotificationServiceImpl.NotificationReplyResponse> getReplyCommentNotification(
            HttpServletRequest request,
            @PathVariable("offset") int offset
    ){
        try{
            int userId = getUserId(request);
            return notificationService.getReplyCommentNotification(userId,offset);
        }catch (ApiException apiException){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,apiException.getErrorCode().getStandardMessage());
        }catch (Exception e){
            logger.error("getReplyCommentNotification fail reason is : {}",e.getMessage(),e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"服务器错误");
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

    @DeleteMapping("/delete/{type}")
    public String delete(HttpServletRequest request,@PathVariable("type") int type){
        try{
            notificationService.deleteNotifications(getUserId(request), NotificationCode.fromCode(type));
            return "ok";
        }catch(Exception e){
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"服务器错误");
        }
    }
}
