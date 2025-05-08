package com.server.controller.api.interaction;

import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.entity.user.UserRelation;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.push.service.NotificationService;
import com.server.service.interaction.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/interaction")
@Tag(name = "互动 api 接口 ",description = "包含用户关系,一键三连的处理")
public class InteractionController {

    @Autowired private InteractionService interactionService;
    @Autowired private NotificationService notificationService;

    private final Logger logger= LoggerFactory.getLogger(InteractionController.class);


    @PutMapping("/handle-user-relation")
    @Operation(summary = "处理用户关系,返回用户操作后的状态")
    private ResponseEntity<Result> HandleUserRelation(HttpServletRequest request,
                                                      @RequestBody UserRelation userRelation){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            userRelation.setUser_id(userId);
            Boolean status= this.interactionService.handleUserRelation(userRelation);

            if(status!=null && status)
                this.notificationService.followToAuthorNotices(userId,userRelation.getTarget_id());

            return Result.Ok(status);
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(" handleUserRelation  fail  reason  is   {} ",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/coin-flip/{videoId}")
    @Operation(summary = "处理投币")
    public ResponseEntity<Result> HandleCoinFlip(HttpServletRequest request,
                                                 @PathVariable("videoId") int videoId){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            this.interactionService.handleCoinForVideo(userId,videoId);

            return Result.Ok("投币成功");
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("/coin-flip fail  reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/handle-favorites/{videoId}/{isFavorite}")
    @Operation(summary = "处理收藏视频",description = "返回修改后的状态 isFavorite=0取消收藏 1收藏")
    public ResponseEntity<Result> HandleFavorite(HttpServletRequest request,
                                                 @PathVariable("videoId") int videoId,
                                                 @PathVariable("isFavorite") int isFavorite){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            boolean status=this.interactionService.handleFavFoeVideo(userId,videoId,isFavorite);

            return Result.Ok(status);
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("/coin-flip fail  reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/handle-like/{videoId}/{authorId}/{like}")
    @Operation(summary = "处理点赞",description = "返回当前状态 isLike=0 取消  1点赞")
    public ResponseEntity<Result> HandleLike(HttpServletRequest request,
                                             @PathVariable("videoId") int videoId,
                                             @PathVariable("authorId") int authorId,
                                             @PathVariable("like") int like){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            boolean status=this.interactionService.handleLikeForVideo(userId,videoId,authorId,like);
            if(status) notificationService.likeToVideoNotices(userId,authorId,videoId);

            return Result.Ok(status);
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("/coin-flip fail  reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
