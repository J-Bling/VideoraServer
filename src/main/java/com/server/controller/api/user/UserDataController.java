package com.server.controller.api.user;

import com.server.dto.response.Result;
import com.server.dto.response.user.UserProfileResponse;
import com.server.dto.response.user.UserResponse;
import com.server.dto.response.video.VideoContributeResponse;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.service.userservice.UserDataService;
import com.server.service.userservice.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.UUID;


/**
 * 1 其他用户信息查询
 * 2 用户信息更改
 * 3 查询用户收藏 点赞 投币的视频
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户数据中心")
public class UserDataController {
    @Autowired private UserService userService;
    @Autowired private UserDataService userDataService;

    @Value("${IMAGE_AVATAR_PREFIX}")
    private String IMAGE_AVATAR_PREFIX;
    @Value("${USER_AVATAR_BASE_URL}")
    private String USER_AVATAR_BASE_URL;

    private final Logger logger= LoggerFactory.getLogger(UserDataController.class);

    private int getUserId(HttpServletRequest request){
        return Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
    }

    @GetMapping("/user-response-data/{targetId}")
    @Operation(summary = "查询用户基本信息 : UserResponse ->{id,nickname,gender,avatar_url,description }")
    public ResponseEntity<Result> getUserResponseData(@PathVariable("targetId") Integer targetId){
        try{
            return Result.Ok(userDataService.getUserResponseData(targetId));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getUserResponseData fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/user-data-stats/{targetId}")
    @Operation(summary = "查询用户信息数据 : UserResponse :base + stats")
    public ResponseEntity<Result> getUserDataWithStats(@PathVariable("targetId") Integer targetId){
        try{
            return Result.Ok(userDataService.getUserDataWithStats(targetId));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getUserDataWithStats fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @GetMapping("/user-stats-relation/{targetId}")
    @Operation(summary = "查询完整用户信息 : UserResponse :base + stats + relation")
    public ResponseEntity<Result> getUserWithStatsAndRelation(
            HttpServletRequest request ,
            @PathVariable("targetId") Integer targetId
    ){
        int userId = getUserId(request);
        try{
            return Result.Ok(userDataService.getUserWithStatsAndRelation(targetId,userId));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getUserWithStatsAndRelation fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }



    @PutMapping("/reset-nickname/{nickname}")
    @Operation(summary = "更改用户名")
    public ResponseEntity<Result> resetNickname(HttpServletRequest request,@PathVariable("nickname") String nickname){
        try{
            if(nickname==null||nickname.length()<6) return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);

            int userId = getUserId(request);
            userService.resetNickname(userId,nickname);
            UserResponse userResponse = userDataService.tryGetUserResponse(userId);
            if(userResponse!=null){
                userResponse.setNickname(nickname);
                userDataService.setUserResponseOnCache(userResponse);
            }

            return Result.Ok("修改成功");

        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("resetNickname fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }



    public static class ResetPasswordRequest{
        private String Account;
        private String code;
        private Boolean type;
        private String oldPassword;
        private String newPassword;

        public boolean isVailToCodeVerification(){
            return Account!=null && code!=null
                    && type!=null && oldPassword !=null
                    && newPassword.length()>7;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public String getOldPassword() {
            return oldPassword;
        }

        public String getCode() {
            return code;
        }

        public String getAccount() {
            return Account;
        }

        public Boolean getType() {
            return type;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public void setType(Boolean type) {
            this.type = type;
        }

        public void setAccount(String account) {
            Account = account;
        }
    }

    @PutMapping("/reset-password/password")
    @Operation(summary = "旧密码 更改密码")
    public ResponseEntity<Result> resetPasswordByPass(
            HttpServletRequest request,
            @RequestBody ResetPasswordRequest passwordRequest
    ){
        try{
            if(passwordRequest.getOldPassword()==null || passwordRequest.getNewPassword()==null){
                return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);
            }

            int userId = getUserId(request);
            userService.resetPasswordByPass(userId,passwordRequest);

            return Result.Ok("修改密码成功");
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("resetPasswordByPass fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @PutMapping("/reset-password/code")
    @Operation(summary = "验证码 更改密码")
    public ResponseEntity<Result> resetPasswordByCode(
            HttpServletRequest request,
            @RequestBody ResetPasswordRequest passwordRequest
    ){
        try{
            if(!passwordRequest.isVailToCodeVerification()){
                return Result.ErrorResult(ErrorCode.BAD_REQUEST,0);
            }
            int userId = getUserId(request);
            userService.resetPasswordByCode(userId,passwordRequest);

            return Result.Ok("修改密码成功");
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("resetPasswordByCode fail reason is : {}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }


    @PutMapping("/reset-avatar")
    @Operation(summary = "更改用户头像")
    public ResponseEntity<Result> resetUserAvatar(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.isEmpty()) {
                return Result.ErrorResult(ErrorCode.BAD_REQUEST, "请上传文件");
            }

            long maxFileSize = 3 * 1024 * 1024; // 3MB = 3 * 1024 * 1024 bytes
            if (file.getSize() > maxFileSize) {
                return Result.ErrorResult(ErrorCode.PAYLOAD_TOO_LARGE, "文件大小不能超过3MB");
            }

            String suffix ;

            String contentType = file.getContentType();
            if(contentType==null) return Result.ErrorResult(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "仅支持JPEG或PNG格式");
            if(contentType.equalsIgnoreCase("image/jpeg")){
                suffix="jpeg";
            }else if(contentType.equalsIgnoreCase("image/png")){
                suffix="png";
            }else {
                return Result.ErrorResult(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "仅支持JPEG或PNG格式");
            }

            int userId = getUserId(request);

            String url = UUID.randomUUID().toString()+userId+suffix;
            String webUrl = IMAGE_AVATAR_PREFIX+url;
            String filename = USER_AVATAR_BASE_URL+url;

            File file1= new File(filename);
            file.transferTo(file1);

            UserResponse userResponse =userDataService.tryGetUserResponse(userId);
            if(userResponse!=null){
                userResponse.setAvatar_url(webUrl);
                userDataService.setUserResponseOnCache(userResponse);
            }

            userService.resetUserAvatar(userId,webUrl);

            return Result.Ok(webUrl);

        } catch (ApiException apiException) {
            return Result.ErrorResult(apiException.getErrorCode(), apiException.getMessage());
        } catch (Exception e) {
            logger.error("resetUserAvatar failed: {}", e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }


    @GetMapping("/profile")
    public UserProfileResponse getProfile(HttpServletRequest request){
        try{
            return userService.getProfile(getUserId(request));
        }catch (Exception e){
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"获取用户数据失败");
        }
    }


    @GetMapping("/collection-video/{offset}")
    public List<VideoDataResponse> getCollection(HttpServletRequest request,@PathVariable("offset") int offset){
        try{
            return userService.getCollection(getUserId(request),offset);
        }catch (Exception e){
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"获取收藏视频数据失败");
        }
    }

    @GetMapping("/contribute-video/{offset}")
    public List<VideoContributeResponse> getContribute(HttpServletRequest request,@PathVariable("offset") int offset){
        try {
            return userService.getContributeVideos(getUserId(request),offset);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"获取投稿视频数据失败");
        }
    }


    @GetMapping("/o-contribute-video/{userId}/{offset}")
    public List<VideoContributeResponse> getContributes(@PathVariable("userId") int userId,@PathVariable("offset") int offset){
        try{
            return userService.getContributeVideos(userId,offset);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"获取投稿视频数据失败");
        }
    }
}
