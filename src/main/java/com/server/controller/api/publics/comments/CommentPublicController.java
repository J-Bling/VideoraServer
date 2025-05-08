package com.server.controller.api.publics.comments;

import com.server.dto.response.Result;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.service.commentservice.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/public/comments")
@Tag(name = "comments 开放 api")
public class CommentPublicController {
    @Autowired
    private CommentService commentService;

    private final int LIMIT=20;

    private final Logger logger= LoggerFactory.getLogger(CommentPublicController.class);

    @GetMapping("/video-comments/{videoId}/{offset}/{limit}")
    @Operation(summary = "获取公开的视频 comments 一级列表")
    public ResponseEntity<Result> getPublicVideoComments(@PathVariable("videoId") int videoId,
                                      @PathVariable("offset") int offset){
        try{
            return Result.Ok(this.commentService.getPublicVideoComments(videoId,offset,LIMIT));
        }catch (ApiException apiException){
          return Result.ErrorResult(apiException.getErrorCode(),0);
        } catch (Exception e){
            logger.error("getPublicVideoComments fail reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/reply-comment/{rootId}/{parentId}/{offset}")
    @Operation(summary = "获取公开回复 comments ")
    public ResponseEntity<Result> getPublicReplyComments(
            @PathVariable("rootId") int rootId,
            @PathVariable("parentId") int parentId,
            @PathVariable("offset") int offset
    ){
        try{
            return Result.Ok(this.commentService.getPublicReplyComments(rootId,parentId,offset,LIMIT));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getPublicReplyComments fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
