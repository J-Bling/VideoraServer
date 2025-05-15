package com.server.controller.api.publics.comments;

import com.server.comment.server.CommentService;
import com.server.dto.response.Result;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/public/comments")
@Tag(name = "comments 开放 api")
public class CommentPublicController {
    @Autowired private CommentService commentService;

    private final Logger logger= LoggerFactory.getLogger(CommentPublicController.class);

    @GetMapping("/video-comments/{videoId}/{offset}")
    public ResponseEntity<Result> getCommentsByVideoId(
            HttpServletRequest request, @PathVariable("videoId") int videoId,
            @PathVariable("offset") int offset
    ){
        try{
            return Result.Ok(commentService.getCommentsByVideoId(videoId,offset,null));
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
            return Result.Ok(commentService.getCommentByVideoWithScore(videoId,null,offset));
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
            return Result.Ok(commentService.getReplyByVideoId(videoId,rootId,null,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
