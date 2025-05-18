package com.server.service.userservice.impl;

import com.server.controller.api.user.UserDataController;
import com.server.dao.stats.UserStatsDao;
import com.server.dao.user.UserDao;
import com.server.dao.user.VerificationCodeDao;
import com.server.dao.video.VideoDao;
import com.server.dto.request.auth.AuthRequest;
import com.server.dto.response.user.UserProfileResponse;
import com.server.dto.response.user.UserResponse;
import com.server.dto.response.video.VideoContributeResponse;
import com.server.dto.response.video.VideoDataResponse;
import com.server.entity.user.User;
import com.server.entity.user.UserStats;
import com.server.entity.video.Video;
import com.server.entity.video.VideoStats;
import com.server.enums.AuthErrorCode;
import com.server.enums.CodeScene;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.exception.AuthException;
import com.server.service.stats.UserStatsService;
import com.server.service.stats.VideoStatsService;
import com.server.service.userservice.UserDataService;
import com.server.service.userservice.UserService;
import com.server.util.AvatarUtil;
import com.server.util.JwtUtil;
import com.server.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    @Autowired private UserDao userDao;
    @Autowired private UserDataService userDataService;
    @Autowired private UserStatsService userStatsService;
    @Autowired private UserStatsDao userStatsDao;
    @Autowired private VerificationCodeDao verificationCodeDao;
    @Autowired private VideoDao videoDao;
    @Autowired private VideoStatsService videoStats;

    private final int LIMIT_VIDEO_SIZE=20;
    private static final String Description="这个人很懒什么都没有写";
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final String CHINA_PHONE_REGEX = "^1[3-9]\\d{9}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern CHINA_PHONE_PATTERN = Pattern.compile(CHINA_PHONE_REGEX);


    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidChinaPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return CHINA_PHONE_PATTERN.matcher(phone).matches();
    }


    @Override
    public boolean deleteUser(int id){
        return this.userDao.deleteUserById(id)>0;
    }

    @Override
    public Integer createUser(AuthRequest request) {
        if(!request.Vail()) throw new AuthException(AuthErrorCode.INVALID_REQUEST);
        if(request.getEmail() !=null) if(isValidEmail(request.getEmail())) throw new AuthException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        if(request.getPhone()!=null) if(isValidChinaPhone(request.getPhone())) throw new AuthException(AuthErrorCode.INVALID_PHONE_FORMAT);
        if (request.getPassword().length()<8) throw new AuthException(AuthErrorCode.INVALID_REQUEST);

        User user=request.toEntity();
        user.setAvatar_url(AvatarUtil.randomAvatar());
        user.setDescription(Description);
        String salt=PasswordUtil.generateSalt();
        String password=PasswordUtil.encrypt(user.getPassword(),salt);
        user.setSalt(salt);
        user.setPassword(password);

        int stats= request.getPhone()==null ? userDao.increaseUserByEmail(user)
                : userDao.increaseUserByPhone(user);

        if(stats<=0) throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        userStatsDao.insertUserStats(user.getId());

        return user.getId();
    }


    private User findUserByPass(AuthRequest request){
        if(request.getPassword()==null) {
            throw new AuthException(AuthErrorCode.INVALID_REQUEST);
        }

        if(request.getEmail() !=null) {
            if (!isValidEmail(request.getEmail())) throw new AuthException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
        else if(request.getPhone() !=null) {
            if (!isValidChinaPhone(request.getPhone())) throw new AuthException(AuthErrorCode.INVALID_PHONE_FORMAT);
        }
        else {
            throw new AuthException(AuthErrorCode.INVALID_REQUEST);
        }
        return request.getEmail()!=null ? userDao.findUserByEmail(request.getEmail())
                : userDao.findUserByPhone(request.getPhone());
    }

    @Override
    public String loginByPass(AuthRequest request) {
        User user = findUserByPass(request);

        if(user==null) throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        if(user.getLocked()) throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);

        String password=PasswordUtil.encrypt(request.getPassword(),user.getSalt());
        if(!user.getPassword().equals(password)) throw new AuthException(AuthErrorCode.INCORRECT_PASSWORD);
        return JwtUtil.generateToken(user.getId(),1);
    }

    @Override
    public User loginByPassGetUser(AuthRequest request){
        return findUserByPass(request);
    }


    @Override
    public String loginByCode(AuthRequest request) {
        if(request.getCode()==null){
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        Integer userId=null;

        if(request.getEmail()!=null) {
            boolean isVail=verificationCodeDao.verifyCodeExists(request.getEmail(), true,request.getCode(), CodeScene.LOGIN.getCode(),System.currentTimeMillis());
            if(isVail) userId=userDao.findUserIdByEmail(request.getEmail());
        }else if (request.getPhone()!=null){
            boolean isVail=verificationCodeDao.verifyCodeExists(request.getPhone(), true,request.getCode(), CodeScene.LOGIN.getCode(),System.currentTimeMillis());
            if(isVail) userId=userDao.findUserIdByPhone(request.getPhone());
        }else {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        if(userId==null) throw new ApiException(ErrorCode.BAD_REQUEST);

        return JwtUtil.generateToken(userId,1);
    }

    @Override
    public User loginByCodeGetUser(AuthRequest request){
        if(request.getCode()==null){
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        if(request.getEmail()!=null) {
            boolean isVail=verificationCodeDao.verifyCodeExists(request.getEmail(), true,request.getCode(), CodeScene.LOGIN.getCode(),System.currentTimeMillis());
            if(isVail) return userDao.findUserByEmail(request.getEmail());

        }else if (request.getPhone()!=null){
            boolean isVail=verificationCodeDao.verifyCodeExists(request.getPhone(), true,request.getCode(), CodeScene.LOGIN.getCode(),System.currentTimeMillis());
            if (isVail) return userDao.findUserByPhone(request.getPhone());
        }
        throw new ApiException(ErrorCode.BAD_REQUEST);
    }

    @Override
    public void resetNickname(int userId, String nickname) {
        userDao.updateNickname(userId,nickname);
    }

    @Override
    public void resetPasswordByPass(int userId, UserDataController.ResetPasswordRequest passwordRequest) {
        User user = userDao.findUserPassword(userId);
        if (user==null){
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        boolean isVailPassword = PasswordUtil.verify(passwordRequest.getOldPassword(),user.getSalt(),user.getPassword());
        if(!isVailPassword) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        String newPassword = PasswordUtil.encrypt(passwordRequest.getNewPassword(),user.getSalt());
        userDao.updatePassword(userId,newPassword);
    }

    @Override
    public void resetPasswordByCode(int userId, UserDataController.ResetPasswordRequest passwordRequest) {
        boolean isVailCode= verificationCodeDao.verifyCodeExists(
                passwordRequest.getAccount(),
                passwordRequest.getType(),
                passwordRequest.getCode(),
                CodeScene.RESET.getCode(),
                System.currentTimeMillis());

        if(!isVailCode){
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        String salt = PasswordUtil.generateSalt();
        String newPassword= PasswordUtil.encrypt(passwordRequest.getNewPassword(),salt);

        userDao.updatePasswordAndSalt(userId,newPassword,salt);
    }

    @Override
    public void resetUserAvatar(int userId, String avatar_url) {
        userDao.updateAvatar(userId,avatar_url);
    }



    @Override
    public UserProfileResponse getProfile(int userId) throws InterruptedException {
        UserProfileResponse response = new UserProfileResponse();
        UserResponse userResponse = userDataService.getUserDataWithStats(userId);
        UserStats userStats = userStatsService.getUserStats(userId);
        response.setUserResponse(userResponse);
        response.setUserStats(userStats);
        response.setLikeVideos(videoDao.getRecentLikeVideo(userId,LIMIT_VIDEO_SIZE));
        response.setCoinVideos(videoDao.getRecentCoinVideo(userId,LIMIT_VIDEO_SIZE));
        return response;
    }

    @Override
    public List<VideoContributeResponse> getContributeVideos(int userId, int offset) throws InterruptedException {
        List<Video> videos = videoDao.getContributeVideo(userId,offset,LIMIT_VIDEO_SIZE);
        if(videos==null || videos.isEmpty()) return Collections.emptyList();

        List<VideoContributeResponse> responses = new ArrayList<>();
        for(Video video : videos){
            VideoStats stats = videoStats.getVideoStats(video.getId());
            VideoContributeResponse response = new VideoContributeResponse();
            response.setVideo(video);
            response.setVideoStats(stats);
            responses.add(response);
        }

        return responses;
    }

    @Override
    public List<VideoDataResponse> getCollection(int userId, int offset) {
        return videoDao.getCollection(userId,offset,LIMIT_VIDEO_SIZE);
    }


    public boolean isAdmin(String sub){
        int id= Integer.parseInt(sub);
        return this.userDao.isAdminById(id);
    }

    public boolean isLock(int id){
        return this.userDao.isLockById(id);
    }
}
