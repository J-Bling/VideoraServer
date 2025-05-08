package com.server.controller.api.comments;

import com.server.dto.request.comment.CommentRequest;
import com.server.dto.request.comment.CommentUserActionRequest;
import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.push.service.NotificationService;
import com.server.service.commentservice.CommentService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Autowired
    private CommentService commentService;

    @Autowired
    private NotificationService notificationService;

    private static final int LIMIT=20;
    private final Logger logger= LoggerFactory.getLogger(CommentController.class);

    @GetMapping("/video-comments/{videoId}/{offset}/{isHot}")
    @Operation(summary = "请求视频下第一级评论" ,description = "默认limit为20,isHo 为true")
    public ResponseEntity<Result> getVideoComments(@PathVariable("videoId") int videoId,
                                                   @PathVariable("offset") Integer offset,
                                                   @PathVariable("isHot") Integer isHot,
                                                   HttpServletRequest request){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            return Result.Ok(this.commentService.getVideoComments(videoId,userId,
                    (offset!=null?offset:0),LIMIT,!isHot.equals(0)));
        }catch (Exception e){
            logger.error("getVideoComments fail reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/reply-comments/{rootId}/{parentId}/{offset}/{isHot}")
    @Operation(summary = "获取下一级回复评论 ")
    public ResponseEntity<Result> getReplyComments(HttpServletRequest request,
                                                   @PathVariable("rootId") Integer rootId,
                                                   @PathVariable("parentId") Integer parentId,
                                                   @PathVariable("offset") Integer offset,
                                                   @PathVariable("isHot") Integer isHot){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            return Result.Ok(this.commentService.getReplyComments(rootId,parentId,userId,
                    (offset!=null?offset:0),LIMIT,!isHot.equals(0)));
        }catch (Exception e){
            logger.error("getReplyComments fail reason is  {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @PostMapping("/create-comment")
    @Operation(summary = "创建评论或者回复")
    public ResponseEntity<Result> publishComment(HttpServletRequest request, @RequestBody CommentRequest commentRequest){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            this.commentService.createComment(commentRequest,userId);
            if(commentRequest.getParent_id()!=null && commentRequest.getParent_id()>0 && commentRequest.getAuthor_id()!=null){
                notificationService.commentToCommentNotices(userId,commentRequest.getAuthor_id(),commentRequest.getParent_id());
            }
            return Result.Ok("succeed");

        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("publishComment fail reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @DeleteMapping("/delete-comment/{commentId}/{videoId}/{parentId}")
    @Operation(summary = "删除评论")
    public ResponseEntity<Result> deleteComment(HttpServletRequest request ,
                                                @PathVariable("commentId") int commentId,
                                                @PathVariable("videoId") int videoId,
                                                @PathVariable("rootId") int rootId,
                                                @PathVariable("parentId") int parentId){
        try{
            int userId=(int) request.getAttribute("id");
            this.commentService.deleteComment(commentId,videoId,rootId,parentId,userId);
            return Result.Ok("succeed");
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("delete comment fail reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @PutMapping("/handle-comment-action")
    @Operation(summary = "互动评论",description = "可以进行取消互动和 点赞 点踩 user_id可以为null")
    public ResponseEntity<Result> handleCommentAction(HttpServletRequest request,
                                                      @RequestBody CommentUserActionRequest commentUserActionRequest){
        try{
            int userId=Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            commentUserActionRequest.setUser_id(userId);
            this.commentService.handleAction(commentUserActionRequest);
            if(commentUserActionRequest.getAction_type())
                notificationService.likeToCommentNotices(userId,commentUserActionRequest.getAuthorId(),commentUserActionRequest.getComment_id());
            return Result.Ok("succeed");

        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("handle comment action fail  reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

}
