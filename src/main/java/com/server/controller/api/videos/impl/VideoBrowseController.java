package com.server.controller.api.videos.impl;

import com.server.dto.response.Result;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.enums.VideoCategory;
import com.server.exception.ApiException;
import com.server.service.stats.VideoStatsService;
import com.server.service.videoservice.VideoService;
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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/video/data")
@Tag(name = "视频数据与推荐接口")
public class VideoBrowseController {
    @Autowired private VideoStatsService videoStatsService;
    @Autowired private VideoService videoService;

    private final Logger logger = LoggerFactory.getLogger(VideoBrowseController.class);

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
            VideoDataResponse response= videoService.getVideoResponseData(videoId,userId);
            videoStatsService.CountView(videoId,1);
            return Result.Ok(response);
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @PostMapping("/video-cover")
    public List<VideoDataResponse> getVideoCover(HttpServletRequest request,@RequestBody Integer[] videoIds){
        try{
            if(videoIds==null || videoIds.length==0) return null;
            List<VideoDataResponse> videoDataResponses = new ArrayList<>();
            for(Integer id : videoIds){
                VideoDataResponse response = videoService.getVideoResponseData(id,null);
                if(response!=null) videoDataResponses.add(response);
            }
            return videoDataResponses;

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"获取视频封面失败");
        }
    }
}
