package com.server.controller.api.videos.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.controller.api.videos.VideoUploadController;
import com.server.dto.request.video.VideoClipUploadRequest;
import com.server.dto.request.video.VideoUploadRequest;
import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.entity.video.Video;
import com.server.entity.video.VideoClip;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.service.videoservice.VideoEditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/videos/upload")
@Tag(name = "Video 创作")
public class VideoUploadImpl implements VideoUploadController {

    @Autowired private VideoEditService videoEditService;

    private final Logger logger= LoggerFactory.getLogger(VideoUploadImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @PostMapping(value = "/init",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传视频初始化",description = "上传初始化成功后 " +
            "会返回videoId过来 并且会设置待审核中 当所有分片完全发送成功即审核成功" +
            ",上传的clip_count 数量需要等于最后一段分片的index")
    public ResponseEntity<Result> updateInit(
            HttpServletRequest request,
            @RequestParam("uploadRequest") String uploadRequestSTr,
            @RequestParam("imageFile") MultipartFile imageFile)
    {
        try{
            VideoUploadRequest uploadRequest ;
            try{
                uploadRequest =mapper.readValue(uploadRequestSTr,mapper.constructType(VideoUploadRequest.class));
            }catch(Exception e){
                return Result.ErrorResult(ErrorCode.BAD_REQUEST,"数据格式错误");
            }

            Integer userId= Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());

            if(!uploadRequest.isVail()) return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);
            Video video= uploadRequest.toEntity();
            video.setAuthor(userId);

            Integer videoId= this.videoEditService.createVideoInit(video,imageFile.getInputStream(),imageFile.getOriginalFilename());
            Map<String,Integer> data=new HashMap<>();
            data.put("videoId",videoId);
            return Result.Ok(data);

        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),"视频上传失败");
        }catch (Exception e){
            logger.error("server fail reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,"服务器错误");
        }
    }

    @Override
    @PostMapping(value = "/chunk",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传视频分片",description = "逐个上传 存储 会返回下一个分片的索引")
    public ResponseEntity<Result> uploadChunk(
            HttpServletRequest request,
            @RequestParam("clipUploadRequest") String clipUploadRequestStr,
            @RequestParam("videoFile") MultipartFile videoFile) {
        try{
            VideoClipUploadRequest clipUploadRequest ;

            try{
                clipUploadRequest = mapper.readValue(clipUploadRequestStr,mapper.constructType(VideoClipUploadRequest.class));
            }catch (Exception e){
                return Result.ErrorResult(ErrorCode.BAD_REQUEST,"数据格式错误");
            }

            if(!clipUploadRequest.isValue()) return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);

            VideoClip videoClip=clipUploadRequest.toEntity();
            Integer nextIndex =this.videoEditService.saveVideoClip(videoClip,videoFile);
            Map<String,Integer> data=new HashMap<>();
            data.put("nextIndex",nextIndex);
            return Result.Ok(data);

        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),"分片上传失败");
        }catch (Exception e){
            logger.error("server fail reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/delete/{videoId}")
    public ResponseEntity<Result> deleteVideo(
            HttpServletRequest request,
            @PathVariable("videoId") Integer videoId
    ){
        try{
            Integer userId= Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            videoEditService.deleteVideoDataForUploadFail(videoId,userId);
            return Result.Ok(1);
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),"分片上传失败");
        }catch (Exception e){
            logger.error("server fail reason is {}",e.getMessage(),e);
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
