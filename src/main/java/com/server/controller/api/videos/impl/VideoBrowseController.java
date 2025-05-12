package com.server.controller.api.videos.impl;

import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.enums.VideoCategory;
import com.server.exception.ApiException;
import com.server.service.videoservice.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/video/data")
@Tag(name = "视频数据与推荐接口")
public class VideoBrowseController {
    @Autowired private VideoService videoService;

    @GetMapping("/recommend/{offset}")
    @Operation(summary = "推荐视频")
    public ResponseEntity<Result> recommendVideos(
            HttpServletRequest request,
            @PathVariable("offset") int offset
    ){
        Integer userId = Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
        try{
            return Result.Ok(videoService.videoRecommendationsByRandom(userId,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/category/{categoryName}/{offset}")
    @Operation(summary = "分类推荐视频")
    public ResponseEntity<Result> categoryVideos(
            HttpServletRequest request,
            @PathVariable("categoryName") String categoryName,
            @PathVariable("offset") int offset
    ){
        Integer userId = Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
        try{
            return Result.Ok(videoService.videoRecommendationsByCategory(userId, VideoCategory.fromName(categoryName),offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/video-clip/{videoId}/{offset}/{quality}")
    @Operation(summary = "获取视频分片")
    public ResponseEntity<Result> getVideoClips(
            @PathVariable("videoId") int videoId,
            @PathVariable("offset") int offset,
            @PathVariable("quality") int quality
    ){
        try{
            return Result.Ok(videoService.getVideoClipUrl(videoId,offset,quality==1));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/detail/{videoId}")
    @Operation(summary = "视频详情信息")
    public ResponseEntity<Result> getVideoDetail(
            HttpServletRequest request,
            @PathVariable("videoId") int videoId
    ){
        Integer userId = Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
        try{
            return Result.Ok(videoService.getVideoResponseData(videoId,userId));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
