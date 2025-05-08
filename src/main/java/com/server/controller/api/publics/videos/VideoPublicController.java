package com.server.controller.api.publics.videos;

import com.server.dto.response.Result;
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

@RestController
@RequestMapping("/api/public/videos")
@Tag(name = "video 开放 api")
public class VideoPublicController {
    @Autowired private VideoService videoService;

    @GetMapping("/recommend/{offset}")
    @Operation(summary = "推荐视频")
    public ResponseEntity<Result> recommendVideos(
            @PathVariable("offset") int offset
    ){
        try{
            return Result.Ok(videoService.videoRecommendationsByRandom(offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/category/{categoryName}/{offset}")
    @Operation(summary = "分类推荐视频")
    public ResponseEntity<Result> categoryVideos(
            @PathVariable("categoryName") String categoryName,
            @PathVariable("offset") int offset
    ){
        try{
            return Result.Ok(videoService.videoRecommendationsByCategory(VideoCategory.fromName(categoryName),offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/video-clip/{videoId}/{offset}")
    @Operation(summary = "获取视频分片")
    public ResponseEntity<Result> getVideoClips(
            @PathVariable("videoId") int videoId,
            @PathVariable("offset") int offset
    ){
        try{
            return Result.Ok(videoService.getVideoClipUrl(videoId,offset,false));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/detail/{videoId}")
    @Operation(summary = "视频详情信息")
    public ResponseEntity<Result> getVideoDetail(
            @PathVariable("videoId") int videoId
    ){
        try{
            return Result.Ok(videoService.getVideoResponseData(videoId,null));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/hottest")
    @Operation(summary = "推荐最热视频")
    public ResponseEntity<Result> getHottestVideo(){
        try{
            return Result.Ok(videoService.getMaxHotVideoData());
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
