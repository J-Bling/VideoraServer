package com.server.controller.api.videos;

import com.server.dto.request.video.VideoClipUploadRequest;
import com.server.dto.request.video.VideoUploadRequest;
import com.server.dto.response.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface VideoUploadController {
    ResponseEntity<Result> updateInit(HttpServletRequest request, String uploadRequest, MultipartFile file);
    ResponseEntity<Result> uploadChunk(HttpServletRequest request, String clipUploadRequest,MultipartFile file);
}
