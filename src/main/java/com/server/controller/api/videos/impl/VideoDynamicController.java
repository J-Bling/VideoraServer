package com.server.controller.api.videos.impl;

import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.service.dynamic.DynamicService;
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
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/video/dynamic")
@Tag(name = "频道")
public class VideoDynamicController {
    @Autowired private DynamicService dynamicService;

    private final Logger logger = LoggerFactory.getLogger(VideoDynamicController.class);

    private int getUserId(HttpServletRequest request){
        return Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
    }

    @GetMapping("/recommend/{lastCreated}")
    @Operation(summary = "推荐频道")
    public ResponseEntity<Result> recommendDynamicVideo(
            HttpServletRequest request,
            @PathVariable("lastCreated") long lastCreated
    ){
        try{
            Timestamp timestamp =lastCreated==0 ? null : new Timestamp(lastCreated);
            int userId =getUserId(request);
            return Result.Ok(dynamicService.findVideoByUserId(userId,timestamp));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/select/{authorId}/{offset}/{lastCreated}")
    @Operation(summary = "自选频道")
    public ResponseEntity<Result> selectDynamicVideoByAuthorId(
            @PathVariable("authorId") int authorId,
            @PathVariable("offset") int offset,
            @PathVariable("lastCreated") long lastCreated
    ){
        try{
            Timestamp timestamp =lastCreated ==0 ? null : new Timestamp(lastCreated);
            return Result.Ok(dynamicService.findVideoDataByAuthorId(authorId,timestamp,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error(e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
